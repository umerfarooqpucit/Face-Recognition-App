# Face-Recognition-App
Simple face recognition android app using OpenCV and JavaCV. Training is done using LBPH face recognizer

# Prerequisites 
Just create these folders faceRecognizer/faces/ in the root directory of you android phone's external storage. Trained faces will be copied to this directory by the app automatically.
Create another folder as faceRecognizer/haar/ in the root directory. Copy the file named "haarcascade_frontalface_alt.xml" from the assets folder of the android project and paste this file in the newly created haar folder.

# How to use
After creating the required folders build the project and run on you phone. You'll see two buttons:
1. Add a new Training face
2. Recognize

If you click button 1 you'll be asked to enter the name of person. After that camera will open and you have to take a selfie. Repeat this 2-3 times (This will crop faces and save them and use them for training later).
If you click button 2 camera will open and you'll again have to take a selfie. This time app will try to make a prediction based on training data. That's it.

Ps. This is just a simple app and will work only if you complete the prerequisites.
