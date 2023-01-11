call curl -X POST http://localhost:5049/manage/shutdown
call mvn clean -Dstyle.color=never install -T 4 -am -DskipTests --projects Cap
move /Y Cap\target\cap-2.0.jar Cap\cap.jar

REM call mvn clean -Dstyle.color=never install -T 4 -am -DskipTests --projects MicroStreamTester
REM move /Y MicroStreamTester\target\microStreamTester-1.0.jar MicroStreamTester\microStreamTester.jar