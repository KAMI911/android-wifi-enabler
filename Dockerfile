# ═══════════════════════════════════════════════════════════════════════════
#  Android Build Environment — Wi-Fi Enabler
#  Provides: JDK 17 + Android SDK (API 35) + Gradle 8.9
#
#  Build:
#    docker build -t android-wifi-enabler-builder .
#
#  Build APK (debug):
#    docker run --rm -v "$(pwd)":/workspace android-wifi-enabler-builder \
#      gradle assembleDebug
#
#  Update SDK / Gradle versions via --build-arg:
#    docker build \
#      --build-arg ANDROID_COMPILE_SDK=36 \
#      --build-arg GRADLE_VERSION=8.10 \
#      -t android-wifi-enabler-builder .
# ═══════════════════════════════════════════════════════════════════════════

FROM eclipse-temurin:17-jdk-jammy

# ── Build arguments ────────────────────────────────────────────────────────
ARG ANDROID_SDK_TOOLS_VERSION=11076708
ARG ANDROID_COMPILE_SDK=35
ARG ANDROID_BUILD_TOOLS=35.0.0
ARG GRADLE_VERSION=8.9

# ── Environment variables ──────────────────────────────────────────────────
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION} \
    GRADLE_OPTS="-Dorg.gradle.daemon=false -Xmx4g -Xms512m" \
    DEBIAN_FRONTEND=noninteractive

ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$GRADLE_HOME/bin

# ── System dependencies ────────────────────────────────────────────────────
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        wget \
        unzip \
        git \
        ca-certificates \
        findutils \
    && rm -rf /var/lib/apt/lists/*

# ── Android SDK command-line tools ─────────────────────────────────────────
# Downloads the Android command-line tools which includes sdkmanager / avdmanager.
RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && wget -q \
       "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_TOOLS_VERSION}_latest.zip" \
       -O /tmp/sdk-tools.zip \
    && unzip -q /tmp/sdk-tools.zip -d /tmp/sdk-tmp \
    && mv /tmp/sdk-tmp/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest" \
    && rm -rf /tmp/sdk-tools.zip /tmp/sdk-tmp

# ── Accept licences and install SDK components ─────────────────────────────
RUN yes | sdkmanager --licenses > /dev/null 2>&1 \
    && sdkmanager \
        "platform-tools" \
        "platforms;android-${ANDROID_COMPILE_SDK}" \
        "build-tools;${ANDROID_BUILD_TOOLS}"

# ── Gradle ─────────────────────────────────────────────────────────────────
# Installing Gradle directly means the container does NOT depend on
# gradle/wrapper/gradle-wrapper.jar being present in the project repo.
# The same Gradle version as the project's wrapper properties is used.
RUN wget -q \
       "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
       -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt/gradle \
    && rm /tmp/gradle.zip

# ── Smoke-test the installation ────────────────────────────────────────────
RUN java -version \
    && gradle --version \
    && sdkmanager --version

# ── Working directory ──────────────────────────────────────────────────────
WORKDIR /workspace

# Mount the project here and run Gradle tasks.
# Example: docker run --rm -v "$(pwd)":/workspace <image> gradle assembleDebug
CMD ["gradle", "--version"]
