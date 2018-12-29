# Face-Recognition-App
Simple face recognition android app using OpenCV and JavaCV. Training is done using LBPH face recognizer

# Note 
You have to Give storage permission to app from settings explicitly for Android 6.0 and above 

# How to use
Build the project and run on you phone. You'll see two buttons:
1. Add a new Training face
2. Recognize

If you click button 1 you'll be asked to enter the name of person. After that camera will open and you have to take a selfie. Repeat this 2-3 times (This will crop faces and save them and use them for training later).
If you click button 2 camera will open and you'll again have to take a selfie. This time app will try to make a prediction based on training data. That's it.
It will save the model when you exit the app and if you re-run the app, it will reload the model.


