# Build image for the Android project (JDK 17 + Android SDK command-line tools).
# linux/amd64 only: build-tools' AAPT2 has no native arm64 Linux binary.
#
# Build:
#   docker build --platform linux/amd64 -t n4j-android-build:amd64 docker/
# Use (see scripts/bundle-release.sh for the signed release bundle):
#   docker run --rm --platform linux/amd64 -v "$PWD":/project \
#     -v n4j-gradle-cache-amd64:/root/.gradle -w /project \
#     n4j-android-build:amd64 ./gradlew --no-daemon assembleDebug testDebugUnitTest
FROM eclipse-temurin:17-jdk

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=/opt/java/openjdk/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools

RUN apt-get update && apt-get install -y --no-install-recommends unzip curl \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools \
    && curl -fsSL -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip \
    && unzip -q /tmp/cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools \
    && mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

# Keep in sync with compileSdk in app/build.gradle.kts.
RUN yes | sdkmanager --licenses >/dev/null 2>&1; \
    sdkmanager --install "platform-tools" "platforms;android-36" "build-tools;36.0.0" "build-tools;35.0.0" >/dev/null

WORKDIR /project
