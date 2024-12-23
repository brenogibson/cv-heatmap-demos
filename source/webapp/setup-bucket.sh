#!/bin/bash

# Check if a bucket name was provided
if [ $# -eq 0 ]; then
    echo "Error: Please provide a bucket name as an argument"
    echo "Usage: ./update_bucket.sh <new-bucket-name>"
    exit 1
fi

NEW_BUCKET_NAME=$1
FILE_PATH="./src/main/java/awsPrototype/metadatas/Constants.java"

# Check if file exists
if [ ! -f "$FILE_PATH" ]; then
    echo "Error: File not found at $FILE_PATH"
    exit 1
fi

# Create a backup of the original file
cp "$FILE_PATH" "${FILE_PATH}.backup"

# Detect OS and use appropriate sed syntax
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    sed -i '' "s/DEFAULT_S3_BUCKET_NAME = \".*\"/DEFAULT_S3_BUCKET_NAME = \"$NEW_BUCKET_NAME\"/" "$FILE_PATH"
else
    # Linux and others
    sed -i "s/DEFAULT_S3_BUCKET_NAME = \".*\"/DEFAULT_S3_BUCKET_NAME = \"$NEW_BUCKET_NAME\"/" "$FILE_PATH"
fi

if [ $? -eq 0 ]; then
    echo "Successfully updated bucket name to: $NEW_BUCKET_NAME"
else
    echo "Error: Failed to update the file"
    # Restore from backup
    mv "${FILE_PATH}.backup" "$FILE_PATH"
    exit 1
fi

# Remove backup file if everything succeeded
rm "${FILE_PATH}.backup"

mvn install
