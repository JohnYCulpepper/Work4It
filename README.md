# Work4It

Work4It is an Android app paired with a Python server that uses MediaPipe pose detection.

- **Frontend:** Android app (open in Android Studio)
- **Backend:** FastAPI server with MediaPipe and OpenCV (run from VS Code or any Python editor)

## Requirements

- Git
- Android Studio
- VS Code (or any Python editor)
- Python 3.10 or newer

## Getting Started

Clone the repo once. It contains both the frontend and the backend.

```
git clone https://github.com/JohnYCulpepper/Work4It.git
```

## Backend Setup (VS Code)

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
   - More info: [MediaPipe Pose Landmarker page](https://ai.google.dev/edge/mediapipe/solutions/vision/pose_landmarker)
   - The file is not stored in the repo because of its size.

5. Start the server:
   ```
   python work4it_server.py
   ```
   The server listens on port `8000`.

## Frontend Setup (Android Studio)

1. Open Android Studio and choose **Open**, then select the `frontend` folder.
2. Wait for Gradle to finish syncing. Dependencies download automatically, and Android Studio will prompt you to install any missing SDK components.
3. Open `config.properties` and set the server address (see below).
4. Run the app on an emulator or a physical device.

## Connecting the App to the Server

The server runs on `0.0.0.0:8000`, but the app cannot connect to `0.0.0.0` directly. Set the address in `config.properties` based on how you run the app:

| Running on | Address |
| --- | --- |
| Android emulator | `10.0.2.2:8000` |
| Physical phone (same Wi-Fi as your computer) | `YOUR-COMPUTER-IP:8000` (find it with `ipconfig` in Terminal) |

Make sure the server is running before you launch the app, and that your firewall allows connections on port 8000.

## Project Structure

```
Work4It/
  backend/     Python server and MediaPipe model
  frontend/    Android Studio project
  README.md
```

## Troubleshooting

- **App can't reach the server:** check the address in `config.properties`, confirm the server is running, and make sure the phone and computer are on the same network.
- **`ModuleNotFoundError` when starting the server:** activate the virtual environment and run `pip install -r requirements.txt` again.
- **Model file not found:** confirm `pose_landmarker_full.task` is in the `backend` folder.
