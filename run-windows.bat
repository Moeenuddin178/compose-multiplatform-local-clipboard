@echo off
echo Starting Local Clipboard App...
echo.
echo Make sure Java is installed on your system.
echo If you don't have Java, download it from: https://www.java.com/download/
echo.
echo Using cross-platform JAR with Windows native libraries included...
echo.
pause
java -jar "desktopApp\build\libs\local-clipboard-cross-platform-1.0.0.jar"
pause
