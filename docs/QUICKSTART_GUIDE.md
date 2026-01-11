# Battlecode 2026 Quickstart Guide

## Overview
This is the Battlecode 2026 contest website, which will be your main hub for all Battlecode-related things for the duration of the contest. For a general overview of what Battlecode is, visit our landing page.

Deep beneath the abandoned dorms of MIT, thanks to a student letting their failed final project loose on campus, a robotic rat society has formed. Like all developing societies, there is, of course, conflict. It is not chromatic, but it is dangerous. You have heard tales of many large, hungry robot cats (someone else's failed project, probably) that are on the prowl for sustenance. As such, your society and a nearby society have formed an uneasy alliance.

Before you begin putting the cat to sleep, you must remember the task you were born with: protect your mother, the big fat rat king.

## Account and Team Creation
To participate in Battlecode, you need an account and a team. Each team can consist of 1 to 4 people.

- Create an account on this website
- Go to the team section to either create or join a team
- If you need to rename your team for any reason, please reach out to Teh Devs on Discord or at battlecode@mit.edu

## Installation and Setup

### Step 1: Install Java and Python
Install both languages, even if you only plan to write a bot in one of them (like most competitors).

#### Java
- You'll need a **Java Development Kit (JDK) version 21**
- Other versions will not work, and note that this is different from the use of Java 8 in Battlecode 2024 and earlier
- Download it [here](https://www.oracle.com/java/technologies/downloads/#java21). You may need to create an Oracle account, or you can use another JDK distribution
- Alternatively, you can install a JDK yourself using your favorite package manager. Make sure it is compatible with Java 21
- If you're unsure how to install the JDK, you can find instructions for all operating systems [here](https://docs.oracle.com/en/java/javase/21/install/overview-jdk-installation.html) (pay attention to PATH and CLASSPATH)

#### Python
- Install **Python 3.12** from [here](https://www.python.org/downloads/) by scrolling down on that page and clicking on the link corresponding to your operating system
- Versions other than 3.12 will not work
- Make sure to check the box that says "Add Python to PATH" during installation
- Python virtual environments (including venv and conda) may be useful if other versions of Python exist on your system, but they are not required
- Once you have installed Python, make sure this Python installation is the one you are using when you run Battlecode (you can check the version with `python --version`)

### Step 2: Download Battlecode
Next, you should download the Battlecode 2026 scaffold. (This step may not work if you are doing this before the Battlecode 2026 kickoff.) To get up and running quickly, you can click "Clone or download" and then "Download ZIP," and move on to the next step.

**We recommend, however, that you instead use Git to organize your code.** If you haven't used Git before, read [this guide](https://git-scm.com/book/en/v2/Getting-Started-About-Version-Control) (or wait for our lecture covering it). On the scaffold page, click "Use this template." **Importantly, on the next page, make your new repo private (you don't want other teams to view your code!).** You can then clone your newly created repo and invite your team members to collaborate on it.

### Step 3: Local Setup
If you are primarily using Java to write your bot, we recommend using an IDE like IntelliJ IDEA or Eclipse to work on Battlecode, but you can also use your favorite text editor combined with a terminal. Visual Studio Code is a useful editor for both Java and Python, but is usually not as powerful as an IDE. Battlecode 2026 uses Gradle to run tasks like run, debug and jarForUpload (but don't worry about that, because you don't need to install it).

#### IntelliJ IDEA
1. Download IntelliJ IDEA Community Edition from [here](https://www.jetbrains.com/idea/download/)
2. In the Welcome to IntelliJ IDEA window that pops up when you start IntelliJ, select **Import Project**
3. In the Select File or Dictionary to Import window, select the `build.gradle` file in the scaffold folder
4. Hit **OK**
5. We need to set the jdk properly; open the settings with **File > Settings** (IntelliJ IDEA > Preferences on Mac) or `ctrl+alt+s`
6. Navigate to **Build, Execution, Deployment > Build Tools > Gradle** and change **Gradle JVM** to **21**
7. Time for a first build! On the right side of the screen, click the small button that says gradle and has a picture of an elephant
8. Navigate to **battlecode26-scaffold > Tasks > battlecode** and double click on **update** and then **build**
9. This will run tests to verify that everything is working correctly, as well as download the client and other resources
10. If you haven't seen any errors, you should be good to go

#### Eclipse
1. Download the latest version of Eclipse from [here](https://www.eclipse.org/downloads/)
2. In the Installer, select **Eclipse IDE for Java Developers**
3. Create a new Eclipse workspace. **The workspace should NOT contain the battlecode26-scaffold folder**
4. Run **File > Import...**, and select **Gradle > Existing Gradle Project**
5. Next to Project root directory field, press **Browse...** and navigate to battlecode26-scaffold. Finish importing the project
6. If you do not see a window labeled Gradle Tasks, navigate to **Window > Show View > Other....** Select **Gradle > Gradle Tasks**
7. In the Gradle Tasks window, you should now see a list of available Gradle tasks. Open the battlecode26-scaffold folder and navigate to the battlecode group, and then double-click **update** and **build**
8. This will run tests to verify that everything is working correctly, as well as download the client and other resources
9. You're good to go; you can run other Gradle tasks using the other options in the Gradle Tasks menu. Note that you shouldn't need any task not in the battlecode group

#### Visual Studio Code
1. Download Visual Studio Code from [here](https://code.visualstudio.com/)
2. Navigate to the Extensions tab using the button on the left side of the screen, and install the "Extension Pack for Java" and "Python" extensions (both made by Microsoft)
3. Open the battlecode26-scaffold folder in VS Code
4. Open a new terminal in VS Code using **Terminal > New Terminal**
5. If you are using a Python virtual environment, activate it in the terminal
6. Use the terminal instructions below to update and build

#### Terminal
1. Start every Gradle command with `./gradlew`, if using Mac or Linux, or `gradlew`, if using Windows
2. You will need to set the `JAVA_HOME` environment variable to point to the installation path of your JDK
3. Navigate to the root directory of your battlecode26-scaffold, and run:
   ```bash
   ./gradlew update
   ./gradlew build
   ```
   (Replace `./gradlew` with `gradlew` on Windows)
4. This will run tests to verify that everything is working correctly, as well as download the client and other resources
5. You're good to go. Run `./gradlew -q tasks` (`gradlew -q tasks` on Windows) to see the other Gradle tasks available. You shouldn't need to use any tasks outside of the battlecode group

There should now be a folder called `client` in your scaffold folder; if you go in there, and double click the Battlecode Client application, you should be able to run and watch matches. **(Please don't move that application, it will be sad.)** If you notice any severe issues with the default client, you can try setting the `compatibilityClient` gradle property to `true` to download the compatibility version.

## Developing Your Bot
Place each version of your robot in a new subfolder in the `src` folder. Make sure every version has a `RobotPlayer.java` or `bot.py` file, depending on the language used. Check debugging tips if you experience problems while developing, or ask on the Discord.

## Running Battlecode

### From the Client
Open the client as described in Step 3. Navigate to the runner tab, select which bots and maps to run, and hit **Run Game!** Finally, click the play/pause button to view the replay. Note that you can also use the web client located at https://play.battlecode.org/bc26/client if you are having issues running the client locally.

### From the Terminal
You can run games directly from the terminal with the Gradle task:

```bash
./gradlew run -Pmaps=[map] -PteamA=[team A] -PteamB=[team B] -PlanguageA=[language A] -PlanguageB=[language B]
```

Replace `[map]`, `[team A]`, `[team B]`, `[language A]`, and `[language B]` with the map name, the names of the bots you want to run, and the languages that the bots are written in. The language names should be formatted as `java` or `python`. If you don't include the map, team, or language flags, Battlecode will default to whatever is listed in `gradle.properties`. Running the same gradle task from your IDE will also work.

### Important Note
**New this year: Cross-play between Java and Python bots is supported.** ALL games can be run using the Gradle command shown above, no matter what the languages of the bots are. There is no separate Python command.

## Common Issues
Check common issues if you experience problems with the instructions above, and if that doesn't help, ask on the Discord.

## Getting Help
- Discord: Ask questions on the Battlecode Discord
- Email: battlecode@mit.edu
- Website: https://play.battlecode.org/bc26
