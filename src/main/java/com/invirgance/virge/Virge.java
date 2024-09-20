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

package com.invirgance.virge;

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
import com.invirgance.virge.tool.Tool;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jbanes
 */
public class Virge
{
    public static final Tool[] tools = new Tool[] {
        new Copy()
    }; 
    
    public static final Map<String,Tool> lookup = new HashMap<>();
    
    static {
        for(Tool tool : tools) lookup.put(tool.getName(), tool);
    }
    
    public static void exit(int code, String message)
    {
        System.err.println(message);
        
        System.exit(code);
    }

    public static void main(String[] args) throws Exception
    {
        Tool tool;
        
        // TODO: Need to print help text
        if(args.length < 1) exit(5, "Need to specify tool");
        
        tool = lookup.get(args[0]);
        
        if(tool == null) exit(6, "Unknown tool: " + args[0]);
        if(!tool.parse(args, 1)) exit(7, "Incorrect parameters for tool " + args[0]); // TODO: Need to print help text
        
        tool.execute();
    }
}
