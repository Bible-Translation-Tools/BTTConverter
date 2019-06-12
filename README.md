# tR_Converter
translationRecorder Converter

# Requirements
Java installed

# How To Use
Open a terminal and run the commands:

*PATH* - a directory that has TranslationRecorder directory in it

### Convert
*a) When you want to convert the old version files to be compatible with the latest version of tR*  
*b) When you want to set or update the mode (verse/chunk) of the project files*

    java -jar tRConverter.jar -d <PATH>

**example:**

    java -jar tRConverter.jar -d C\Users\User\Desktop
Assuming that we have TranslationRecorder directory in our desktop folder

### Transform
*a) When you want to change the language of the project files and/or resource type*

*LANGUAGE_CODE* - language code (defaults to 'en' if not specified)  
*LANGUAGE_NAME* - original language name (optional). It's used for TranslationExchange transfered projects  
*VERSION* - resource type code (ulb, udb, reg - defaults to 'ulb' if not specified)  

    java -jar tRConverter.jar -t -d <PATH> -lc <LANGUAGE_CODE> -ln <LANGUAGE_NAME> -v <VERSION> 

**NOTICE**: if you want to update just a resource type, you need to set the original language code as well, otherwise it will change to 'en' (English). Same, when you want to update just the language, you need to also set the original resource type, otherwise it will change to ‘ulb’

**example:**

    java -jar tRConverter.jar -t -d C\Users\User\Desktop -lc ru
This will change the language of the files to Russian and the resource type to ULB (because the resource type was not specified)

    java -jar tRConverter.jar -t -d C\Users\User\Desktop -lc es -v reg

This will change the language of the files to Spanish and the resource type to REG

