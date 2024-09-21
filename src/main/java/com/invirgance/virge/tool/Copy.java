/*
 * Copyright 2024 INVIRGANCE LLC

Permission is hereby granted, free of charge, to any person obtaining a copy 
of this software and associated documentation files (the “Software”), to deal 
in the Software without restriction, including without limitation the rights to 
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies 
of the Software, and to permit persons to whom the Software is furnished to do 
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
SOFTWARE.
 */
package com.invirgance.virge.tool;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.input.BSONInput;
import com.invirgance.convirgance.input.DelimitedInput;
import com.invirgance.convirgance.input.Input;
import com.invirgance.convirgance.input.JSONInput;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.output.BSONOutput;
import com.invirgance.convirgance.output.DelimitedOutput;
import com.invirgance.convirgance.output.JSONOutput;
import com.invirgance.convirgance.output.Output;
import com.invirgance.convirgance.source.FileSource;
import com.invirgance.convirgance.source.InputStreamSource;
import com.invirgance.convirgance.source.Source;
import com.invirgance.convirgance.target.FileTarget;
import com.invirgance.convirgance.target.OutputStreamTarget;
import com.invirgance.convirgance.target.Target;
import com.invirgance.convirgance.transform.CoerceStringsTransformer;
import com.invirgance.virge.Virge;

import static com.invirgance.virge.Virge.exit;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

/**
 *
 * @author jbanes
 */
public class Copy implements Tool
{
    private Source source;
    private Input<JSONObject> input;

    private Target target;
    private Output output;
    
    private char inputDelimiter;
    private char outputDelimiter;
    private boolean bsonCompress;
    private boolean detectTypes;

    public Source getSource()
    {
        return source;
    }

    public void setSource(Source source)
    {
        this.source = source;
    }

    public Input<JSONObject> getInput()
    {
        return input;
    }

    public void setInput(Input<JSONObject> input)
    {
        this.input = input;
    }

    public Target getTarget()
    {
        return target;
    }

    public void setTarget(Target target)
    {
        this.target = target;
    }

    public Output getOutput()
    {
        return output;
    }

    public void setOutput(Output output)
    {
        this.output = output;
    }
    
    private boolean isURL(String path)
    {
        char c;
        
        if(!path.contains(":/")) return false;
        if(path.charAt(0) == ':') return false;
        
        for(int i=0; i<path.length(); i++)
        {
            c = path.charAt(i);
            
            if(c == ':') return (path.charAt(i+1) == '/');
                
            if(!Character.isLetter(c)) return false;
        }
        
        return false;
    }
    
    private Source getSource(String path) throws MalformedURLException, IOException
    {
        File file;
        
        if(isURL(path))
        {
            return new InputStreamSource(URI.create(path).toURL().openStream());
        }
        
        file = new File(path);
        
        if(!file.exists()) throw new ConvirganceException("File not found: " + path);
        
        return new FileSource(file);
    }
    
    // TODO: Improve auto-detection
    private Input<JSONObject> detectInput(String path) throws MalformedURLException
    {
        if(isURL(path))
        {
            path = URI.create(path).toURL().getFile();
        }
        
        path = path.toLowerCase();
        
        if(path.endsWith(".json")) return new JSONInput();
        if(path.endsWith(".csv")) return new DelimitedInput(','); // TODO: need to support proper CSV format
        if(path.endsWith(".bson")) return new BSONInput();
        
        return null;
    }
    
    private Input<JSONObject> getInput(String type)
    {
        switch(type)
        {
            case "csv": // TODO: need to support proper CSV format
                return new DelimitedInput(',');
            
            case "tsv":
                return new DelimitedInput('\t');
            
            case "pipe":
                return new DelimitedInput('|');
            
            case "delimited":
                
                if(inputDelimiter != 0) return new DelimitedInput(inputDelimiter);
                
                return new DelimitedInput();
            
            case "bson":
                return new BSONInput();
                
            case "json":
                return new JSONInput();
                
            default:
                exit(255, "Unknown input type: " + type);
                return null; // Keep the compiler happy
        }
    }
    
    private Target getTarget(String path) throws MalformedURLException, IOException
    {
        File file;
        
        if(isURL(path))
        {
            return new OutputStreamTarget(URI.create(path).toURL().openConnection().getOutputStream());
        }
        
        file = new File(path);
        
        if(!file.getParentFile().exists()) file.getParentFile().mkdirs();
        
        return new FileTarget(file);
    }
    
    // TODO: Should the default be symetrical input/output?
    private Output detectOutput(String path) throws MalformedURLException
    {
        if(isURL(path))
        {
            path = URI.create(path).toURL().getFile();
        }
        
        path = path.toLowerCase();
        
        if(path.endsWith(".json")) return new JSONOutput();
        if(path.endsWith(".csv")) return new DelimitedOutput(','); // TODO: need to support proper CSV format
        if(path.endsWith(".bson")) return new BSONOutput(bsonCompress);
        
        return null;
    }
    
    private Output getOutput(String type)
    {
        switch(type)
        {
            case "csv": // TODO: need to support proper CSV format
                return new DelimitedOutput(',');
            
            case "tsv":
                return new DelimitedOutput('\t');
            
            case "pipe":
                return new DelimitedOutput('|');
            
            case "delimited":
                
                if(outputDelimiter != 0) return new DelimitedOutput(outputDelimiter);
                
                return new DelimitedOutput();
            
            case "bson":
                return new BSONOutput(bsonCompress);
                
            case "json":
                return new JSONOutput();
                
            default:
                exit(255, "Unknown output type: " + type);
                return null; // Keep the compiler happy
        }
    }
    
    @Override
    public String getName()
    {
        return "copy";
    }

    @Override
    public String[] getHelp()
    {
        return new String[] {
            "copy [options] <source> <target>",
            "    Copy a file while performing a tranformation between formats as required",
            "",
            "    --input <format>",
            "    -i <format>",
            "        Specify the format of the input file. Currently supported options are json, csv, tsv, pipe, delimited, and bson",
            "",
            "    --output <format>",
            "    -o <format>",
            "         Specify the format of the output file. Currently supported options are json, csv, tsv, pipe, delimited, and bson",
            "",
            "    --bson-compress",
            "    -z",
            "         Enable compression when writing a bson file",
            "",
            "    --input-delimiter <delimiter>",
            "    -D <delimiter>",
            "         Set the column delimiter if the source is a delimited file (e.g. , or |)",
            "",
            "    --output-delimiter <delimiter>",
            "    -d <delimiter>",
            "         Set the column delimiter if the target is a delimited file (e.g. , or |)",
            "",
            "    --detect-types",
            "    -I",
            "         Attempts to automatically coerce strings in the input records into numbers and booleans. Useful for delimited file inputs or where type information was lost.",
            "",
            "    --source <file path>",
            "    -s <file path>",
            "         Alternate method of specifying the source file",
            "",
            "    --target",
            "    -t <file path>",
            "         Alternate method of specifying the target file",
        };
    }

    private boolean error(String message)
    {
        System.err.println(message);
        
        return false;
    }
    
    @Override
    public boolean parse(String[] args, int start) throws MalformedURLException, IOException
    {
        for(int i=start; i<args.length; i++)
        {
            // Handle single-letter params with no spaces in them
            if(args[i].length() > 2 && args[i].charAt(0) == '-' && Character.isLetterOrDigit(args[i].charAt(1)))
            {
                parse(new String[]{ args[i].substring(0, 2), args[i].substring(2) }, 0);
                
                continue;
            }
            
            switch(args[i])
            {
                case "--help":
                case "-h":
                case "-?":
                    return false;
                
                case "--bson-compress":
                case "-z":
                    bsonCompress = true;
                    
                    if(output instanceof BSONOutput) ((BSONOutput)output).setCompressed(bsonCompress);
                    
                    break;
                
                case "--input-delimiter":
                case "-D":
                    inputDelimiter = args[++i].charAt(0);
                    
                    if(input instanceof DelimitedInput) ((DelimitedInput)input).setDelimiter(inputDelimiter);
                    
                    break;
                    
                case "--output-delimiter":
                case "-d":
                    outputDelimiter = args[++i].charAt(0);
                    
                    if(output instanceof DelimitedOutput) ((DelimitedOutput)output).setDelimiter(outputDelimiter);
                    
                    break;
                
                case "--source":
                case "-s":
                    source = getSource(args[++i]);
                    
                    if(input == null) input = detectInput(args[i]);
                    
                    break;
                    
                case "--input":
                case "--input-type":
                case "-i":
                    input = getInput(args[++i]);
                    
                    break;
                    
                case "--target":
                case "-t":
                    target = getTarget(args[++i]);
                    
                    if(output == null) output = detectOutput(args[i]);
                    
                    break;
                    
                case "--output":
                case "--output-type":
                case "-o":
                    output = getOutput(args[++i]);
                    
                    break;
                    
                case "--detect-types":
                case "-I":
                    detectTypes = true;
                    break;
                    
                default:
                    
                    if(source == null)
                    {
                        source = getSource(args[i]);
                    
                        if(input == null) input = detectInput(args[i]);

                        break;
                    }
                    else if(target == null)
                    {
                        target = getTarget(args[i]);

                        if(output == null) output = detectOutput(args[i]);

                        break;
                    }
                    else
                    {
                        exit(255, "Unknown parameter: " + args[i]);
                    }
            }
        }
        
        if(source == null) return error("No source specified!");
        if(input == null) return error("No input type specified and unable to autodetect");
        if(target == null) return error("No target specified!");
        if(output == null) return error("No output type specified and unable to autodetect");
        
        return true;
    }

    @Override
    public void execute()
    {
        Iterable<JSONObject> iterable;
        
        if(source == null) Virge.exit(254, "No source specified!");
        if(input == null) Virge.exit(254, "No input type specified and unable to autodetect");
        if(target == null) Virge.exit(254, "No target specified!");
        if(output == null) Virge.exit(254, "No output type specified and unable to autodetect");
        
        iterable = input.read(source);
        
        if(detectTypes) iterable = new CoerceStringsTransformer().transform(iterable);
        
        output.write(target, iterable);
    }
}
