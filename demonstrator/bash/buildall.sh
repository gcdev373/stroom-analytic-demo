echo Please run this command from a Java8 shell

echo Building EventGen
cd ../event-gen
./gradlew fatJar
echo
echo Building StateMonitor
cd ../state-monitor
./gradlew fatJar
