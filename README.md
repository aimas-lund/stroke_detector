# Passive Stroke Detector

Project Description Passively monitoring a smartphone user, using the front camera, to check whether a stroke is occurring. This will be done by passively checking for facial drooping, which is when one side of the face drops down, compared to the opposite side. When one/multiple positive samples occur, the app will recommend further tests to be performed. The app will then evaluate based on the results, if the user should do an emergency call. This is useful, given time is an important factor for preventing permanent brain damage or death. Moreover, 75% of all cases of stroke are experienced by people who have not experienced stroke before. This means that people’s ability to self-diagnose is poor. This app will help with the self-diagnosis, and improve response time.


### Running the Application
To begin running the Passive Stroke Detector app, first open up the application.

1. To begin running in the background, the user is only required to press the "START SERVICE" button as shown below.
![Home screen](https://user-images.githubusercontent.com/32109231/71318298-b14b1f80-24c1-11ea-97e1-26b24a53757d.png)

2. After pressing "START SERVICE", a notification should appear showing that the app is running in the background.
![Background Service](https://user-images.githubusercontent.com/32109231/71318300-b14b1f80-24c1-11ea-9ddd-4bc0551ae628.png)

3. The user is no longer required to do anything else. If a possible stroke is detected, an alert should appear notifying the user
![Notification](https://user-images.githubusercontent.com/32109231/71318301-b1e3b600-24c1-11ea-8221-856e26ae4576.png)

If the user wishes to stop running the service, the "STOP SERVICE" button should be used and the background service will be killed.

** The Train/Load/Delete Model buttons are only for future improvements for the model. These buttons are not required to run or use the application.
