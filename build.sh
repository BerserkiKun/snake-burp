#!/bin/bash

# Snake Burp Extension - Compilation Script for Mac
# Uses Burp Suite Community Edition JAR directly

echo "🐍 Snake Burp Extension - Compilation Script (Mac)"
echo "==================================================="

# Configuration
PROJECT_DIR="$(pwd)"
CLASSES_DIR="$PROJECT_DIR/target/classes"
OUTPUT_DIR="$PROJECT_DIR/target"
JAR_NAME="snake-burp-v1.0.0.jar"

# Path to Burp Suite Community Edition on Mac
BURP_JAR="/Applications/Burp Suite Professional.app/Contents/Resources/app/burpsuite_pro.jar"
if [ ! -f "$BURP_JAR" ]; then
    BURP_JAR="/Applications/Burp Suite Community Edition.app/Contents/Resources/app/burpsuite_community.jar"
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}📁 Project directory: $PROJECT_DIR${NC}"
echo -e "${YELLOW}📁 Using Burp JAR: $BURP_JAR${NC}"

# Check if Burp JAR exists
if [ ! -f "$BURP_JAR" ]; then
    echo -e "${RED}❌ Burp Suite Community Edition JAR not found at:${NC}"
    echo "   $BURP_JAR"
    echo ""
    echo "Please check if:"
    echo "   1. Burp Suite Community Edition is installed"
    echo "   2. The path is correct"
    echo "   3. You have permissions to access the file"
    echo ""
    echo "Alternative: Enter the correct path to your Burp JAR:"
    read -p "Path: " CUSTOM_BURP_JAR
    
    if [ -f "$CUSTOM_BURP_JAR" ]; then
        BURP_JAR="$CUSTOM_BURP_JAR"
        echo -e "${GREEN}✅ Using Burp JAR from: $BURP_JAR${NC}"
    else
        echo -e "${RED}❌ File not found: $CUSTOM_BURP_JAR${NC}"
        exit 1
    fi
fi

# Create output directories
echo -e "\n${YELLOW}📁 Creating output directories...${NC}"
mkdir -p "$CLASSES_DIR"
mkdir -p "$OUTPUT_DIR"

# Find all Java files (including those in burp/ subdirectories)
echo -e "${YELLOW}🔍 Finding Java source files...${NC}"
JAVA_FILES=$(find "$PROJECT_DIR" -name "*.java" -not -path "*/target/*" -not -path "*/\.*")
if [ -z "$JAVA_FILES" ]; then
    echo -e "${RED}❌ No Java files found in $PROJECT_DIR${NC}"
    exit 1
fi

# Count Java files
JAVA_COUNT=$(echo "$JAVA_FILES" | wc -l | tr -d ' ')
echo -e "${GREEN}✅ Found $JAVA_COUNT Java files:${NC}"
echo "$JAVA_FILES" | while read -r file; do
    echo "   📄 ${file#$PROJECT_DIR/}"
done

# Compile Java files using Burp JAR as classpath
echo -e "\n${YELLOW}⚙️  Compiling Java files...${NC}"
javac -d "$CLASSES_DIR" \
      -cp "$BURP_JAR" \
      -source 21 \
      -target 21 \
      $JAVA_FILES

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Compilation failed${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Compilation successful${NC}"
echo -e "${GREEN}   Classes output to: $CLASSES_DIR${NC}"

# Create manifest file
echo -e "\n${YELLOW}📝 Creating manifest file...${NC}"
cat > "$OUTPUT_DIR/MANIFEST.MF" << EOF
Manifest-Version: 1.0
Created-By: Snake Burp Extension
Extension-Name: Snake 🐍
EOF

# Create JAR file with proper package structure
echo -e "${YELLOW}📦 Creating JAR file...${NC}"
jar cfm "$OUTPUT_DIR/$JAR_NAME" "$OUTPUT_DIR/MANIFEST.MF" -C "$CLASSES_DIR" .

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ JAR creation failed${NC}"
    exit 1
fi

# Check if JAR was created
if [ -f "$OUTPUT_DIR/$JAR_NAME" ]; then
    JAR_SIZE=$(du -h "$OUTPUT_DIR/$JAR_NAME" | cut -f1)
    echo -e "${GREEN}✅ JAR created successfully: $OUTPUT_DIR/$JAR_NAME ($JAR_SIZE)${NC}"
    
    # Show JAR contents summary
    echo -e "\n${YELLOW}📋 JAR package structure:${NC}"
    jar tf "$OUTPUT_DIR/$JAR_NAME" | grep -E "burp/(engine|input|model|ui)/.*\.class" | head -15
    TOTAL_CLASSES=$(jar tf "$OUTPUT_DIR/$JAR_NAME" | grep "\.class$" | wc -l | tr -d ' ')
    echo -e "${GREEN}   Total classes: $TOTAL_CLASSES${NC}"
else
    echo -e "${RED}❌ JAR file not found after creation${NC}"
    exit 1
fi

echo -e "\n${GREEN}🎉 Compilation complete!${NC}"
echo "==================================================="
echo -e "${GREEN}📦 JAR location: $OUTPUT_DIR/$JAR_NAME${NC}"
echo ""
echo "📝 To load in Burp Suite:"
echo "   1. Open Burp Suite Community Edition"
echo "   2. Go to Extender → Extensions"
echo "   3. Click 'Add'"
echo "   4. Extension Type: Java"
echo "   5. Select the JAR file: $OUTPUT_DIR/$JAR_NAME"
echo "   6. Click 'Next' to load"
echo ""
echo "🎮 Enjoy your Snake game in Burp Suite!"