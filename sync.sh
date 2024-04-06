#!/bin/bash

SOURCE_DIR=$(pwd)
DESTINATION_DIR="growpie@192.168.68.69:/home/growpie/growpie"

fswatch --event Created --event Updated --event Removed --event Renamed --recursive "$SOURCE_DIR" | while read -r event; do
    echo "Change detected: $event"
    rsync -avz --delete --exclude '.*' --exclude 'build/' "$SOURCE_DIR/" "$DESTINATION_DIR"
done
