
@REM adjust this to your RXTXcomm.jar. If you installed RXTXcomm.jar in 
@REM jar/lib/ext, leave the following line out.
@REM set _CP=%_CP%;C:\path\to\RXTXcomm.jar

set CLASSPATH=%CLASSPATH%;tc65sh.jar

@REM adjust this to reflect your java installation
@REM set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_16


@REM run tc65sh in interactive mode
@REM "%JAVA_HOME%\bin\java.exe" org.tc65sh.Main -p COM1 -b 115200

@REM run tc65sh in batch mode with a command line
@REM "%JAVA_HOME%\bin\java.exe" org.tc65sh.Main -p COM1 -b 115200 -c "ls;mkdir temp;ls"

@REM run tc65sh in batch mode with a command file
@REM "%JAVA_HOME%\bin\java.exe" org.tc65sh.Main -p COM1 -b 115200 -f cmd.txt


@REM run tc65sh as invoked by the user
"%JAVA_HOME%\bin\java.exe" org.tc65sh.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

pause

