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
import com.invirgance.convirgance.input.JSONInput;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.source.ClasspathSource;
import com.invirgance.virge.tool.*;
import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 *
 * @author jbanes
 */
public class Virge
{
    private static List<JSONObject> modules = new ArrayList<JSONObject>();

    public static final Tool[] tools = new Tool[] {
        new Copy(),
        new GenerateTable(),
        new LoadTable(),
        new SQLDrivers()
    }; 
    
    public static final Map<String,Tool> lookup = new HashMap<>();
    
    static {
        for(Tool tool : tools) lookup.put(tool.getName(), tool);
    }
    
    static {
        JSONInput jsonInput = new JSONInput();
        Iterator<JSONObject> iterator = jsonInput.read(new ClasspathSource("/modules.json")).iterator();

        while(iterator.hasNext()) modules.add(iterator.next());
    }
    
    public static void exit(int code, String message)
    {
        System.err.println(message);
        
        System.exit(code);
    }
    
    private static void print(String[] lines, PrintStream out)
    {
        for(String line : lines)
        {
            out.println(line);
        }
        
        out.println();
        out.println();
    }
    
    public static void printHelp(Tool selected)
    {
        //Note: This currently only prints the help for internal tools 
        
        System.out.println();
        System.out.println("Usage: virge.jar <COMMAND | MODULE>");
        System.out.println();
        System.out.println("Commands:");
        System.out.println();
        
        if(selected != null)
        {
            print(selected.getHelp(), System.out);
        }
        else
        {
            for(Tool tool : tools) print(tool.getHelp(), System.out);
        }
        
        System.exit(1);
    }
    
    public static void printModuleHelp(Tool module, String name)
    {
        if(module == null) printHelp(null);
        
        System.out.println();
        System.out.println("Usage: virge.jar " + name + " <COMMAND>");
        System.out.println();
        System.out.println("Commands:");
        System.out.println();
        
        print(module.getHelp(), System.out);
        
        System.exit(1);
    }
    
    public static void printShortHelp()
    {
        System.out.println();
        System.out.println("Usage: virge.jar <COMMAND | MODULE>");
        System.out.println();
        System.out.println("Commands:");
        System.out.println();
        
        
        for(Tool tool : tools) System.out.println("    " + tool.getHelp()[0]);
        
        System.out.println("\nModules:\n");
        
        for(JSONObject module : modules) 
        {
            System.out.println("    " + module.get("help"));
        }
        
        System.out.println();
        System.exit(1);
    }
    
    public static void hideLoggingError()
    {
        if(System.getProperty("org.slf4j.simpleLogger.defaultLogLevel") == null)
        {
            System.getProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
            
            System.setErr(new PrintStream(System.err) {
                private int counter;
                
                @Override
                public void println(String str)
                {
                    if(str.startsWith("SLF4J: ") && counter < 3)
                    {
                        counter++;
                        
                        return;
                    }
                    
                    super.println(str);
                }
                
            });
        };
    }
    
    private static URL[] translate(File[] files)
    {
        URL[] urls = new URL[files.length];
        
        try
        {
            for(int i=0; i<files.length; i++)
            {
                urls[i] = files[i].toURI().toURL();
            }
        }
        catch(MalformedURLException e) { throw new ConvirganceException(e); }
        
        return urls;
    }
    
    private static boolean loadModule(String[] args) throws Exception
    {
        boolean central;
        ConfigurableMavenResolverSystem maven;
        Class clazz;
        URLClassLoader loader;
        File[] files;

        JSONObject module = null;
        
        String moduleOption = args[0];
        String[] arguments;
        
        for(JSONObject option: modules)
        {
            if(option.get("name").equals(moduleOption))
            {
                module = option;
                break;
            }
        }
        
        // return to main and load through Tools[]        
        if(module == null) return false;
        
        hideLoggingError();
        maven = Maven.configureResolver();
               
        arguments = Arrays.copyOfRange(args, 1, args.length);
        
        central = !Boolean.parseBoolean(System.getProperty("virge.localrepo", "false"));
        files = maven.withMavenCentralRepo(central).resolve(module.getJSONArray("artifact")).withTransitivity().asFile();
        loader = new URLClassLoader(translate(files));

        clazz = loader.loadClass(module.get("main").toString());
        clazz.getMethod("main", String[].class).invoke(null, (Object) arguments);
        
        return true;
    }
    
    public static void main(String[] args) throws Exception
    {
        Tool tool;
        
        // TODO: Need to print help text
        if(args.length < 1) printShortHelp();
        
        if(args[0].equals("--help") || args[0].equals("-h") || args[0].equals("-?"))
        {
            printHelp(null);
        }
        
        if(loadModule(args)) return;
        
        tool = lookup.get(args[0]);
        
        if(tool == null) exit(6, "Unknown tool: " + args[0]);
        
        if(!tool.parse(args, 1)) printHelp(tool);
        
        tool.execute();
    }
}
