#!/usr/bin/env bash
# Reproduz o ambiente de compilação Linux, sem alterar código, planilha ou lockfiles.
set -euo pipefail
repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tools_dir=/workspace/toolchains
mkdir -p "$tools_dir" /workspace/.gradle /workspace/.android
jdk_dir="$tools_dir/jdk-17.0.21+9"
if [[ ! -x "$jdk_dir/bin/javac" ]]; then
  jdk_url='https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21%2B9/OpenJDK17U-jdk_x64_linux_hotspot_17.0.21_9.tar.gz'
  curl --fail --location --retry 2 "$jdk_url" -o "$tools_dir/jdk.tar.gz"
  printf '%s  %s\n' '8b9b611d917e3dfd8419d3566b5949226f69b052fed7e109c1368add1dfe8711' "$tools_dir/jdk.tar.gz" | sha256sum -c -
  tar -xzf "$tools_dir/jdk.tar.gz" -C "$tools_dir"
fi
export JAVA_HOME="$jdk_dir" ANDROID_HOME=/workspace/android-sdk
export GRADLE_USER_HOME=/workspace/.gradle ANDROID_USER_HOME=/workspace/.android
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
# Use the platform's existing trusted Java CA store; do not disable TLS verification.
if [[ -r /etc/ssl/certs/java/cacerts ]]; then
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
# Preserve other Gradle settings. Only manage proxy host/port and the platform trust store.
python3 - <<'PY'
import os,pathlib,urllib.parse
p=pathlib.Path('/workspace/.gradle/gradle.properties')
managed={}
for scheme in ('http','https'):
 v=os.environ.get(scheme.upper()+'_PROXY') or os.environ.get(scheme+'_proxy')
 if v:
  u=urllib.parse.urlparse(v)
  if u.hostname and u.port:
   managed[f'systemProp.{scheme}.proxyHost']=u.hostname
   managed[f'systemProp.{scheme}.proxyPort']=str(u.port)
if pathlib.Path('/etc/ssl/certs/java/cacerts').is_file():
 managed['systemProp.javax.net.ssl.trustStore']='/etc/ssl/certs/java/cacerts'
lines=p.read_text().splitlines() if p.exists() else []
lines=[line for line in lines if line.split('=',1)[0].strip() not in managed]
p.write_text('\n'.join(lines+[f'{k}={v}' for k,v in managed.items()])+'\n')
PY
if [[ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]]; then
  curl --fail --location --retry 2 https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o "$tools_dir/cmdtools.zip"
  # SHA-1 published in Google's repository2-1.xml, verified over HTTPS.
  printf '%s  %s\n' 'd313adb7aedccf6cf0cfca51ec180f0059f5f8f8' "$tools_dir/cmdtools.zip" | sha1sum -c -
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  unzip -q "$tools_dir/cmdtools.zip" -d "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi
sdkmanager="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
# yes may receive SIGPIPE after sdkmanager exits; retain sdkmanager's actual status.
set +o pipefail
yes 2>/dev/null | "$sdkmanager" --sdk_root="$ANDROID_HOME" --licenses > "$tools_dir/licenses.log"
license_status=${PIPESTATUS[1]}
set -o pipefail
[[ "$license_status" -eq 0 ]]
"$sdkmanager" --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
# Retained activation file: processes are not expected to survive a cloud snapshot.
cat > "$tools_dir/wellness-env.sh" <<'ENV'
export JAVA_HOME=/workspace/toolchains/jdk-17.0.21+9
export ANDROID_HOME=/workspace/android-sdk
export ANDROID_USER_HOME=/workspace/.android
export ANDROID_AVD_HOME=/workspace/.android/avd
export GRADLE_USER_HOME=/workspace/.gradle
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
ENV
cd "$repo_dir"
./gradlew --console=plain :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
printf '\nAPK: %s/app/build/outputs/apk/debug/app-debug.apk\n' "$repo_dir"
