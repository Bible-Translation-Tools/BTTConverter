# tR Converter
translationRecorder Converter

# Requirements
Java installed

# How To Use
Open a terminal and run the commands:

*-d PATH* - a destination directory that contains project files in it

### Convert
*a) When you want to convert the old version files to be compatible with the latest version of tR*  
*b) When you want to set or update the mode (verse/chunk) of the project files*

    java -jar tRConverter.jar -d <PATH>

**example:**

    java -jar tRConverter.jar -d C\Users\User\Desktop\Projects

### Transform
*a) When you want to change the language of the project files and/or resource type*

*-p PROJECT* - project directory (it's usually an original language code)
*-lc LANGUAGE_CODE* - language code
*-ln LANGUAGE_NAME* - original language name (optional). It's used for TranslationExchange transfered projects  
*-v VERSION* - resource type (version) code. For example: ulb, udb, reg  

    java -jar tRConverter.jar -t -d <PATH> -p <PROJECT> -lc <LANGUAGE_CODE> -ln <LANGUAGE_NAME> -v <VERSION>

**NOTICE**: Add parameter -t in the command to transform

**example:**

    java -jar tRConverter.jar -t -d C\Users\User\Desktop\Projects -p ua -lc ru
This will change the language of the Ukrainian (ua) project to Russian

    java -jar tRConverter.jar -t -d C\Users\User\Desktop\Projects -p pt-br -lc es -v reg

This will change the language of the Brazilian project to Spanish and the resource type to REG

