Virge unlocks the power of the [Convirgance library](https://github.com/InvirganceOpenSource/convirgance) for the command line interface.

## Basic usage

    java -jar virge.jar copy [options] <source> <target>
    
    <source> - File to read from. Format will be autodetected based on extension.
    <target> - File to write to. Format will be autodetected based on extension.

    [options]

    --input or -i
         Specify the format of the input file. Currently supported options are
         json, csv, tsv, pipe, delimited, and bson

    --output or -o
         Specify the format of the output file. Currently supported options are
         json, csv, tsv, pipe, delimited, and bson

    --bson-compress or -z
         Enable compression when writing a bson file

    --input-delimiter or -D
         Set the column delimiter if the source is a delimited file (e.g. , or |)

    --output-delimiter or -d
         Set the column delimiter if the target is a delimited file (e.g. , or |)

    
