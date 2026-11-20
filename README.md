# Work4It

Work4It is an Android app paired with a Python server that uses MediaPipe pose detection to create a fitness-driven, social media app block that trades physical exercise for screentime.

- **Frontend:** Android app (open in Android Studio)
- **Backend:** FastAPI server with MediaPipe and OpenCV (run from VS Code or any Python editor)

## Requirements

- Git
- Android Studio
- VS Code (or any Python editor; VS code is recommended)
- Python 3.10 or newer

## Getting Started

Clone the repo once. It contains both the frontend and the backend.

```
git clone https://github.com/JohnYCulpepper/Work4It.git
```

## Backend

### Explanation

**POST /count:** takes a frame (JPEG image), `exercise_type` (squat or pushup), and `session_id` (randomly generated unique id) as form data. It returns `{rep_count, stage, exercise_type}` back to Android side for live rep count storage.

**GET /reset:** takes session_id as a query parameter. Resets the rep count and stage. The app calls it when the user stops recording.

**How reps are counted:** Tracks landmark positions returned by MediaPipe to calculate angle (via `distance()` and `calculate_angles()`) at compound movement joints (e.g. knee or elbow).
1. Squats use the knee angle (<= 90° is down, >= 170° is up). 
2. Pushups use the elbow angle (<= 120° is closed, >= 170° is open). Check: requires the shoulders to drop by at least 10% of torso length, so arm wiggles don’t count.

**Unique User ID:** get_counter() function stores and retrieves unique user id (generated from App Side). Allows for multi-user usage rather than relying on a global counter variable, which limits the server to only one user.

**Limitations:** a joint is ignored unless its visibility is above 0.6. Idle sessions expire after 30 minutes, and restarting the server clears all counts

### Setup

1. Open the `backend` folder in VS Code.
2. Open a terminal in that folder and create a virtual environment.

   Windows:
   ```
   python -m venv .venv
   .venv\Scripts\activate
   ```

   Mac/Linux:
   ```
   python3 -m venv .venv
   source .venv/bin/activate
   ```

3. Install dependencies:
   ```
   pip install -r requirements.txt
   ```

4. Download the MediaPipe pose model and place it in the `backend` folder:
   - File: `pose_landmarker_full.task` (~23 MB)
   - Download: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task
   - Place the `pose_landmarker_full.task` file within `frontend/app/src/main/assets/com/johnculpepper`
   - More info: [MediaPipe Pose Landmarker page](https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker)
   - The file is not stored in the repo because of its size.

5. Start the server:
   ```
   python work4it_server.py
   ```
   The server listens on port `8000`.

## Frontend (Android Studio)

### Explanation

**AppBlockerService:** An Android Accessibility Service that watches for application changes. When a blocked app opens (checked against the on/off settings saved in SharedPreferences), it counts down the user's earned screen time once per second while they stay in that app. When the time runs out, or the user opens a blocked app with none left, it launches `BlockOverlayActivity`. The apps that are blocked are determined on the `PickAppsToBlock` activity.

**BlockOverlayActivity:** The block screen shown when a blocked app is opened with no screen time left. It has one button that sends the user to `ExerciseActivity` to earn more time.

**CameraActivity:** The workout screen. It runs the front camera and uses MediaPipe on the phone to draw the skeleton overlay. While recording, it sends JPEG frames at an interval based on the exercise to the Python server. It then shows the rep count that the server returns. When recording stops, it converts the reps into minutes through `TimeManager` and calls the server's reset endpoint.

**ExerciseActivity:** The exercise menu. Tapping Squat or Pushup opens `CameraActivity` with the chosen exercise type attached. A Home button goes back to `MainActivity`.

**MainActivity:** The home screen. It shows the remaining screen time, resets the daily allowance on a new day, and prompts for the required permissions (Accessibility, Usage Access, Display Over Apps). If it is the first time that user has opened the app, it redirects them to `PickAppsToBlock`. Buttons lead to the exercise menu and the block app picker, and there is a dev button for resetting time. 

**OverlayView:** A custom view drawn on top of the camera preview. It takes the pose landmarks from MediaPipe and draws red dots on the shoulders, elbows, wrists, hips, knees, and ankles, with blue lines between them. It hides low-visibility points, rotates the coordinates to match the portrait screen, and mirrors the drawing for the front camera.

**PickAppsToBlock:** The setup screen for choosing which apps to block (Instagram, TikTok, YouTube, Facebook), using one `SlideButton` per app. It also checks permissions. Live updates blocking status before even pressing Continue. Tapping Continue directs user to `MainActivity`.

**SlideButton:** A custom toggle switch with a track and a sliding thumb. It loads its on/off state from SharedPreferences using the app's package name as the key, animates when toggled, and saves the new state. `AppBlockerService` reads that saved value to decide what to block.

**TimeManager:** The helper that stores the user's screen-time balance in SharedPreferences. It tracks seconds remaining and minutes earned today, adds minutes from exercise, subtracts seconds while a blocked app is in use, and caps earnings at 2 hours per day. It also has a daily reset.

### Setup

1. Open Android Studio and choose **Open**, then select the `frontend` folder.
2. Wait for Gradle to finish syncing. Dependencies download automatically, and Android Studio will prompt you to install any missing SDK components.
3. Open `configure.properties` and set the server address (see below).
4.  Download the same MediaPipe pose model and place it in the `frontend/app/src/main/assets/com/johnculpepper/Work4It/pose_landmarker_full.task` file path:
   - Download: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task
   - More details found in **Backend Setup 4.**
5. Run the app on an emulator or a physical device.

## Connecting the App to the Server

The server runs on `0.0.0.0:8000`, but the app cannot connect to `0.0.0.0` directly. Set the address in [`configure.properties`](https://github.com/JohnYCulpepper/Work4It/tree/master/frontend/app/src/main/assets/com/johnculpepper/Work4It) based on how you run the app:

| Running on | Address |
| --- | --- |
| Android emulator | `10.0.2.2:8000` |
| Physical phone (same Wi-Fi as your computer) | `YOUR-COMPUTER-IP:8000` (find it with `ipconfig` in Terminal) |

What it currently looks like in `configure.properties`: `http://YOUR-COMPUTER-IP:8000/count`

Due note that you would also have to change the try-catch `YOUR-COMPUTER-IP` statement within [`CameraActivity.java`](https://github.com/JohnYCulpepper/Work4It/blob/master/frontend/app/src/main/java/com/johnculpepper/work4it/CameraActivity.java) the catch block in onCreate.

Make sure that the server is running when you try the app, and your firewall allows connections on port 8000.

## Project Structure

```
Work4It/
  backend/     Python server and MediaPipe model
   requirements.txt
   work4it_server.py
  frontend/    Android Studio project
   ...
   app/
   ...
  README.md
```

## Troubleshooting

- **App can't reach the server:** check the address in `configure.properties`, confirm the server is running, and make sure the phone and computer are on the same network.
- **`ModuleNotFoundError` when starting the server:** activate the virtual environment and run `pip install -r requirements.txt` again.
- **Model file not found:** confirm `pose_landmarker_full.task` is in both the `backend` and `frontend` folders.
