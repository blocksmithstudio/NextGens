#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Additional Maven options (for example -o) can be passed as arguments.
bash ./mvnw -B -ntp "$@" verify \
  org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -Dmdep.outputFile=target/regression-classpath.txt

java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
classpath="target/classes:$(cat target/regression-classpath.txt)"

# Keep the standalone CI checks in the same local verification command.
for check in HologramProviderCheck RefundCheck; do
  "$java_cmd" "scripts/$check.java"
done

# Exercise the built classes with disposable database and scheduler fixtures.
for check in LifecycleRegression StartupLoadingRegression ShutdownRegression; do
  "$java_cmd" -cp "target/test-classes:$classpath" "com.muhammaddaffa.nextgens.$check"
done

# Compile only these production classes with isolated test doubles for the server.
output=target/permissions-pagination-check
mkdir -p "$output"
sources=()
while IFS= read -r source; do sources+=("$source"); done < <(find src/test/regression/permissions-pagination -name '*.java' -type f | sort)
for source in autosell/Autosell autosell/AutosellManager \
  sell/multipliers/providers/PermissionSellMultiplierProvider users/UserManager \
  users/models/User utils/Utils gui/helpers/Pagination; do
  sources+=("src/main/java/com/muhammaddaffa/nextgens/$source.java")
done
"$javac_cmd" -cp "$classpath" -d "$output" "${sources[@]}"
"$java_cmd" -cp "$output:$classpath" PermissionsPaginationRegression

for check in SellingCheck GeneratorActionsCheck MenuCheck; do
  "$java_cmd" "src/test/checks/$check.java" "$classpath"
done
