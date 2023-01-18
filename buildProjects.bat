REM call curl -X POST http://localhost:5049/manage/shutdown
REM call mvn clean -Dstyle.color=never install -T 4 -am -DskipTests --projects Cap
REM move /Y Cap\target\cap-2.0.jar Cap\cap.jar

call mvn clean -Dstyle.color=never install -T 4 -am -DskipTests --projects MicroStreamTester
move /Y MicroStreamTester\target\microStreamTester-1.0.jar MicroStreamTester\microStreamTester.jar