#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$DIR/build"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
BUILD_TOOLS="$SDK/build-tools/34.0.0"
ANDROID_JAR="$SDK/platforms/android-34/android.jar"
KEYSTORE="${WETTER_KEYSTORE:?Pfad zum privaten APK-Signierschlüssel angeben}"
: "${WETTER_KEY_PASSWORD:?Passwort für die Signatur fehlt}"
OUTPUT_APK="$DIR/wetter-v1.6.apk"

rm -rf "$WORK"
mkdir -p "$WORK/bin" "$WORK/gen" "$WORK/compiled_res"

echo "==> 1. Compiling resources with aapt2..."
"$BUILD_TOOLS/aapt2" compile --dir "$DIR/app/src/main/res" -o "$WORK/compiled_res.zip"

echo "==> 2. Linking resources and generating R.java..."
"$BUILD_TOOLS/aapt2" link \
    -I "$ANDROID_JAR" \
    --manifest "$DIR/app/src/main/AndroidManifest.xml" \
    --java "$WORK/gen" \
    -o "$WORK/unaligned.apk" \
    --auto-add-overlay \
    "$WORK/compiled_res.zip"

echo "==> 3. Compiling Java sources..."
find "$DIR/app/src/main/java" "$WORK/gen" -name "*.java" > "$WORK/sources.txt"
javac --release 8 -cp "$ANDROID_JAR" -d "$WORK/bin" @"$WORK/sources.txt"

echo "==> 4. Converting bytecode to DEX with d8..."
find "$WORK/bin" -name "*.class" > "$WORK/classes.txt"
"$BUILD_TOOLS/d8" --output "$WORK" --lib "$ANDROID_JAR" @"$WORK/classes.txt"

echo "==> 5. Adding classes.dex to APK..."
cd "$WORK"
python3 -c "import zipfile; z = zipfile.ZipFile('unaligned.apk', 'a'); z.write('classes.dex', 'classes.dex'); z.close()"
cd "$DIR"

echo "==> 6. Aligning APK with zipalign..."
"$BUILD_TOOLS/zipalign" -p -f 4 "$WORK/unaligned.apk" "$WORK/aligned.apk"

echo "==> 7. Signing APK with apksigner..."
"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-pass env:WETTER_KEY_PASSWORD \
    --key-pass env:WETTER_KEY_PASSWORD \
    --out "$OUTPUT_APK" \
    "$WORK/aligned.apk"

echo "==> 8. Verifying APK signature..."
"$BUILD_TOOLS/apksigner" verify --verbose "$OUTPUT_APK"

echo "✅ SUCCESS! Native APK built at: $OUTPUT_APK"
