#!/bin/bash
# Check if app is running
xcrun devicectl device list devices | grep "00008110-001248641A47801E"
echo "Checking app status..."
xcrun devicectl device process launch --device 00008110-001248641A47801E org.clipboard.app.iosApp 2>&1
sleep 1
xcrun devicectl device list processes --device 00008110-001248641A47801E | grep -i clipboard || echo "App not found in process list"
