# Local Clipboard App - Windows Guide

This guide will help you run the Local Clipboard application on your Windows machine.

## 🚀 How to Run

There are two main ways to run the application:

### Method 1: Using the `run-windows.bat` script (Recommended for ease of use)

1.  **Download the necessary files:**
   *   `local-clipboard-cross-platform-1.0.0.jar` (the main application file with Windows native libraries)
   *   `run-windows.bat` (a script to easily launch the application)
   *   `WINDOWS-README.md` (this guide)

2.  **Place them in the same folder.** For example, create a folder named `LocalClipboard` on your desktop and put all three files inside it.

3.  **Ensure Java is installed:**
   *   The application requires Java Runtime Environment (JRE) version 8 or higher.
   *   If you don't have Java installed, you can download it from the official website: [https://www.java.com/download/](https://www.java.com/download/)
   *   The `run-windows.bat` script will try to detect Java. If it's not found, it will prompt you to install it.

4.  **Run the application:**
   *   Simply **double-click** on the `run-windows.bat` file.
   *   A command prompt window will open, and the application should start.

### Method 2: Using the Command Line

1.  **Download `local-clipboard-cross-platform-1.0.0.jar`** and place it in a folder (e.g., `C:\LocalClipboard`).

2.  **Ensure Java is installed** (as described in Method 1, step 3).

3.  **Open Command Prompt:**
   *   Press `Win + R`, type `cmd`, and press `Enter`.

4.  **Navigate to the folder** where you saved `local-clipboard-cross-platform-1.0.0.jar`:
   ```cmd
   cd C:\LocalClipboard
   ```
   (Replace `C:\LocalClipboard` with your actual path)

5.  **Run the application:**
   ```cmd
   java -jar local-clipboard-cross-platform-1.0.0.jar
   ```

## 📋 Requirements

*   **Java Runtime Environment (JRE) 8 or higher.**
    *   You can download it from [https://www.java.com/download/](https://www.java.com/download/)

## ✨ Features

*   **Cross-Platform:** Works on Windows, macOS, and Linux
*   **Native Libraries:** Includes Windows-specific native libraries (Skiko) for optimal performance
*   **Dark Theme:** Toggle between light and dark mode in the Settings.
*   **Enhanced UI:** Modern Material Design 3 interface.
*   **Device Discovery:** Automatically find and connect to other devices running the app.
*   **Clipboard History:** Keep a history of copied items.
*   **Cross-platform Sharing:** Share clipboard content between Windows, macOS, Linux, Android, and iOS devices.
*   **Persistent Settings:** Your preferences are saved across app restarts.

## 🔧 Technical Details

*   **File Size:** ~77 MB (includes all platform-specific native libraries)
*   **Native Libraries:** Windows DLLs, Linux SO files, and macOS DYLIB files included
*   **Compatibility:** Windows 7+ (with Java 8+)

---

Enjoy using the Local Clipboard App!