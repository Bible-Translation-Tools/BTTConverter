# BTT Converter
btt Converter

# Requirements
Java installed

# How To Use
Open a terminal and run the commands:

*-d PATH* - a destination directory that contains project files in it

### Convert
*a) When you want to convert the old version files to be compatible with the latest version of BTT Recorder*  
*b) When you want to set or update the mode (verse/chunk) of the project files*

    java -jar tRConverter.jar -d <PATH>

**example:**

    java -jar tRConverter.jar -d C\Users\User\Desktop\Projects

The command line will offer you to set/change the mode for every book found. Press 1 to set mode as Verse, 2 - for Chunk. If it shows "Current mode" and you want to leave it without changing it, just confirm by pressing 1 or 2 accordingly.

### Transform
*a) When you want to change the language of the project files and/or resource type*

*-pl PROJECT_LANGUAGE* - source project language directory (ex. en, ru, es...)  
*-pv PROJECT_VERSION* - source project version directory (ex. ulb, udb, reg...)  
*-pb PROJECT_BOOK* - source project book (ex. gen, mrk, jas...) Optional. If not specified, all the books will be transformed
*-lc LANGUAGE_CODE* - language code to change to. If omitted, won't be changed   
*-ln LANGUAGE_NAME* - original language name (optional). It's used for TranslationExchange transferred projects  
*-v VERSION* - resource type (version) code to change to. For example: ulb, udb, reg. If omitted, won't be changed   

    java -jar tRConverter.jar -t -d <PATH> -pl <PROJECT_LANGUAGE> -pv <PROJECT_VERSION> -pb <PROJECT_BOOK> -lc <LANGUAGE_CODE> -ln <LANGUAGE_NAME> -v <VERSION>

**NOTICE**: Add parameter -t in the command to transform

**example:**

    java -jar tRConverter.jar -t -d C\Users\User\Desktop\Projects -pl ua -pv ulb -lc ru
This will change the language of the Ukrainian ULB (ua) project (all books) to Russian

    java -jar tRConverter.jar -t -d C\Users\User\Desktop\Projects -pl pt-br -pv ulb -pb mrk -lc es -v reg

This will change the language of the Brazilian project, book of Mark to Spanish and the resource type to REG

