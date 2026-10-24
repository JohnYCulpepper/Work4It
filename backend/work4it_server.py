import cv2
import numpy as np
import mediapipe as mp
from fastapi import FastAPI, File, UploadFile, Form
from pydantic import BaseModel
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
import uvicorn
from pathlib import Path


def distance(
    a: list[int],
    b: list[int]
    ) -> float:
    return np.sqrt((a[0] - b[0])**2 + (a[1] - b[1])**2)

def calculate_angles(
        limb: list[float],
        ) -> float:
    a = distance(limb[1], limb[2])  # elbow to wrist
    b = distance(limb[0], limb[2])   # shoulder to wrist
    c = distance(limb[0], limb[1])    # shoulder to elbow

    denom = 2 * a * c
    if denom == 0:
        return 0.0
    cos_val = np.clip((a**2 + c**2 - b**2) / denom, -1.0, 1.0) # Limits cosine value to its range [-1, 1]
    angle = np.arccos(cos_val)
    return angle

class RepCounter:
    def __init__(self):
        self.count = 0
        self.stage = "none"
        
        self.shoulder_baseline = None
        self.torso_length = None
        self.drop_ratio = 0.10 # buffer percentage
    
    def update(self, angles, exercise_type, visibilities, shoulder_ys = None, hip_ys = None):
        if exercise_type == 'pushup':
            left_visible = visibilities[0] > 0.6
            right_visible = visibilities[1] > 0.6

            if left_visible and right_visible:
                elbow_closed = angles[0] < 110.0 and angles[1] < 110.0
                elbow_opened = angles[0] > 170.0 and angles[1] > 170.0
                avg_shoulder = (shoulder_ys[0] + shoulder_ys[1]) / 2
                avg_hip = (hip_ys[0] + hip_ys[1]) / 2
            elif left_visible:
                elbow_closed = angles[0] < 110.0
                elbow_opened = angles[0] > 170.0
                avg_shoulder = shoulder_ys[0]
                avg_hip = hip_ys[0]
            elif right_visible:
                elbow_closed = angles[1] < 110.0
                elbow_opened = angles[1] > 170.0
                avg_shoulder = shoulder_ys[1]
                avg_hip = hip_ys[1]
            else:
                return

            print(f"PUSHUP: elbow_closed={elbow_closed}, elbow_opened={elbow_opened}\navg_shoulder={avg_shoulder}, baseline={self.shoulder_baseline}, \nstage={self.stage}")

            if elbow_opened:
                # last iteration shoulder y-coordinate
                self.shoulder_baseline = avg_shoulder
                self.torso_length = abs(avg_hip - avg_shoulder)
                if self.stage == "down":
                    self.count += 1
                self.stage = "up"
            elif elbow_closed:
                if self.shoulder_baseline is not None and self.torso_length: # checking existences
                    drop_threshold = self.torso_length * self.drop_ratio
                    # checks whether user shoulders move: new < old - buffer
                    # inversed due to medipipe system (top-to-bottom y coords: 0 to 1)
                    if avg_shoulder > self.shoulder_baseline + drop_threshold:
                        self.stage = "down"
            
        elif exercise_type == 'squat':
            left_visible = visibilities[0] > 0.6
            right_visible = visibilities[1] > 0.6
            print(f"LV: {left_visible}, RV: {right_visible}")

            if left_visible and right_visible:
                going_down = angles[0] < 90.0 and angles[1] < 90.0
                going_up = angles[0] > 170.0 and angles[1] > 170.0
            elif left_visible:
                going_down = angles[0] < 90.0
                going_up = angles[0] > 170.0
            elif right_visible:
                going_down = angles[1] < 90.0
                going_up = angles[1] > 170.0
            else:
                print(f"SQUAT: No visibility, vis: {visibilities}" )
                return

            print(f"SQUAT: angles={angles}, going_down={going_down}, going_up={going_up}, stage={self.stage}")
                  
            if going_down:
                self.stage = "down"
            if going_up:
                if self.stage == "down":
                    self.count += 1
                self.stage = "up"

class RepResponse(BaseModel):
    rep_count: int
    stage: str
    exercise_type: str

counter: RepCounter = RepCounter()

MODEL_PATH = Path(__file__).parent / "pose_landmarker_full.task"
base_options = python.BaseOptions(model_asset_path=str(MODEL_PATH))
options = vision.PoseLandmarkerOptions(
    base_options=base_options,
    output_segmentation_masks=True)
detector = vision.PoseLandmarker.create_from_options(options)

# Initializes FastAPI routes
app = FastAPI()

# Resets exercise count and stage of motion after user reopens app
@app.get("/reset")
async def reset_counter():
    counter.count = 0
    counter.stage = "none"

    return {"message": "Counter reset"}

@app.post("/count")
async def count_reps(
    frame: UploadFile = File(...),
    exercise_type: str = Form(...)):

    print(f"Received frame for exercise: {exercise_type}")
    print(f"Current rep count: {counter.count}")
    print(f"Current stage: {counter.stage}")

    # Mediapipe live processing
    content = await frame.read()
    nparr = np.frombuffer(content, np.uint8)
    img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    # Convert cv2 image to MediaPipe format
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=img)

    # Run detection
    detection_result = detector.detect(mp_image)
    print(f"Pose detected: {bool(detection_result.pose_landmarks)}")
    if not detection_result.pose_landmarks:
        return RepResponse(rep_count=counter.count, stage=counter.stage or "none", exercise_type = exercise_type)

    # Reference sheet of all app's exercises (Not Used)
    # exercise_list = ['squat', 'pushup']
    limb_coords = list()
    visibilities = [0.0, 0.0]

    shoulder_ys = None
    hip_ys = None
    
    # Exercise type landmark extraction
    landmarks = detection_result.pose_landmarks[0]
    if exercise_type == 'squat':
        hip_left = [landmarks[23].x, landmarks[23].y]
        knee_left = [landmarks[25].x, landmarks[25].y]
        ankle_left = [landmarks[27].x, landmarks[27].y]
        hip_right = [landmarks[24].x, landmarks[24].y]
        knee_right = [landmarks[26].x, landmarks[26].y]
        ankle_right = [landmarks[28].x, landmarks[28].y]
        # Stores limb coords into limb coords: [left, right]
        limb_coords.append([hip_left,knee_left,ankle_left])
        limb_coords.append([hip_right,knee_right,ankle_right])

        visibilities = [landmarks[25].visibility,landmarks[26].visibility]

    elif exercise_type == 'pushup':
        shoulder_left = [landmarks[11].x, landmarks[11].y]
        shoulder_right = [landmarks[12].x, landmarks[12].y]
        elbow_left = [landmarks[13].x, landmarks[13].y]
        elbow_right = [landmarks[14].x, landmarks[14].y]
        wrist_left = [landmarks[15].x, landmarks[15].y]
        wrist_right = [landmarks[16].x, landmarks[16].y]
        hip_left = [landmarks[23].x, landmarks[23].y]
        hip_right = [landmarks[24].x, landmarks[24].y]

        limb_coords.append([shoulder_left, elbow_left, wrist_left])
        limb_coords.append([shoulder_right, elbow_right, wrist_right])

        visibilities = [landmarks[13].visibility, landmarks[14].visibility]

        shoulder_ys = [landmarks[11].y, landmarks[12].y]
        hip_ys = [landmarks[23].y, landmarks[24].y]

    angles = list() # [left, right]
    for side in limb_coords:
        angles.append(calculate_angles(side))
    print(f"Computed angles before update(): {angles}, visibilities: {visibilities}")


    # Update RepCounter
    counter.update(angles, exercise_type, visibilities, shoulder_ys, hip_ys)

    # Return response
    return RepResponse(rep_count=counter.count, stage=counter.stage or "none", exercise_type=exercise_type)

if __name__ == '__main__':
    uvicorn.run(app, host="0.0.0.0", port=8000)