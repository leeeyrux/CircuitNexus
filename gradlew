#!/bin/sh
# Lanzador de Gradle para CircuitNexus (Linux/macOS/GitHub Actions)
set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

if ! command -v "$JAVACMD" >/dev/null 2>&1; then
  echo "ERROR: no se encontró Java. Instala un JDK 17 o define JAVA_HOME." >&2
  exit 1
fi

exec "$JAVACMD" -Xmx64m -Xms64m \
  -Dorg.gradle.appname=gradlew \
  -classpath "$WRAPPER_JAR" \
  org.gradle.wrapper.GradleWrapperMain "$@"
