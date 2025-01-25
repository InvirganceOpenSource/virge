/*
 * The MIT License
 *
 * Copyright 2025 jbanes.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.invirgance.virge.tool;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.virge.jdbc.JDBCDrivers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author jbanes
 */
public class SQLDrivers implements Tool
{
    private static final String[] COMMANDS = new String[]{
        "list",
        "register",
        "unregister"
    };
    
    private String command = "list";
    private String driver;
    
    private String name;
    private String datasource;
    private List<String> artifact = new ArrayList<>();
    private List<String> prefix = new ArrayList<>();
    private List<String> shortName = new ArrayList<>();
    private List<String> example = new ArrayList<>();;

    @Override
    public String getName()
    {
        return "sqldrivers";
    }

    @Override
    public String[] getHelp()
    {
        return new String[] {
            "sqldrivers [list|register] [options]",
            "",
            "",
            "    list",
            "        Lists available jdbc drivers for connecting to databases. This is",
            "        the default command if no command is specfied.",
            "",
            "        --driver <driver>",
            "        -d <driver>",
            "            The long name or short name of the driver",
            "",
            "",
            "    register",
            "        --name <name>",
            "        -n <name>",
            "            Set the name of the driver. If the name matches an existing",
            "            driver, the existing driver will be updated.",
            "",
            "        --artifact <groupId:artifactId:version>",
            "        -a <groupId:artifactId:version>",
            "            The Maven coordinates of the JDBC driver. This option can be",
            "            specified more than once if multiple JARs are needed.",
            "",
            "        --driver <className>",
            "        -d <className",
            "            The class name of the JDBC Driver implementation.",
            "",
            "        --data-source <className>",
            "        -D <className",
            "            The class name of the JDBC DataSource implementation. If",
            "            not specified, a default Data Source wrapping the Driver",
            "            will be used.",
            "",
            "        --prefix <url prefix>",
            "        -p <url prefix>",
            "            The url prefix used by this driver. e.g. jdbc:oracle:",
            "            This option can be specified more than once if multiple",
            "            prefixes are supported.",
            "",
            "        --short-name <name>",
            "        -s <name>",
            "            Add a short name for this driver",
            "",
            "",
            "    unregister",
            "        Removes the specified driver from the available database",
            "        drivers.",
            "",
            "        --driver <driver>",
            "        -d <driver>",
            "            The long name or short name of the driver ",
            "",
        };
    }

    @Override
    public boolean parse(String[] args, int start) throws Exception
    {
        for(int i=start; i<args.length; i++)
        {
            if(i == start && Arrays.asList(COMMANDS).contains(args[i]))
            {
                this.command = args[i];
                continue;
            }
            
            switch(args[i])
            {
                case "--driver":
                case "-d":
                    this.driver = args[++i];
                    break;
                    
                case "--data-source":
                case "-D":
                    this.datasource = args[++i];
                    break;
                    
                case "--name":
                case "-n":
                    this.name = args[++i];
                    break;
                    
                case "--artifact":
                case "-a":
                    this.artifact.add(args[++i]);
                    break;
                    
                case "--prefix":
                case "-p":
                    this.prefix.add(args[++i]);
                    break;
                    
                case "--short-name":
                case "-k":
                    this.shortName.add(args[++i]);
                    break;
                    
                case "--example":
                case "-e":
                    this.example.add(args[++i]);
                    break;
                
                default:
                    return false;
            }
        }
        
        return true;
    }
    
    private String format(JSONArray<String> list)
    {
        StringBuffer buffer = new StringBuffer();
        
        for(String item : list)
        {
            if(buffer.length() > 0) buffer.append(",");
            
            buffer.append(item);
        }
        
        return buffer.toString();
    }
    
    private String formatWidth(String value, int width)
    {
        while(value.length() < width) value += " ";
        
        return value;
    }
    
    private String drawWidth(char c, int width)
    {
        StringBuffer buffer = new StringBuffer();
        
        while(buffer.length() < width) buffer.append(c);
        
        return buffer.toString();
    }

    @Override
    public void execute() throws Exception
    {
        if(command.equals("register")) registerDriver();
        else if(command.equals("unregister")) unregisterDriver(driver);
        else if(driver != null) printDriver(driver);
        else printAll();
    }
    
    public void unregisterDriver(String driver)
    {
        JDBCDrivers drivers = new JDBCDrivers();
        JSONObject descriptor = drivers.getDescriptor(driver);
        
        if(descriptor == null)
        {
            System.err.println("Driver '" + driver + "' not found!");
            System.exit(1);
        }
        
        drivers.deleteDescriptor(descriptor);
    }
    
    private void add(JSONArray<String> array, List<String> addition)
    {
        for(String item : addition)
        {
            if(!array.contains(item)) array.add(item);
        }
    }
    
    public void registerDriver()
    {
        JDBCDrivers drivers = new JDBCDrivers();
        JSONObject descriptor = drivers.getDescriptor(name);
        
        if(descriptor == null) 
        {
            descriptor = new JSONObject(true);
            
            descriptor.put("name", name);
            descriptor.put("keys", new JSONArray());
            descriptor.put("artifact", new JSONArray());
            descriptor.put("driver", "");
            descriptor.put("datasource", "com.invirgance.virge.jdbc.DriverDataSource");
            descriptor.put("prefixes", new JSONArray());
            descriptor.put("examples", new JSONArray());
        }
        
        descriptor.put("name", name);
        
        if(driver != null) descriptor.put("driver", driver);
        if(datasource != null) descriptor.put("datasource", datasource);
        
        add(descriptor.getJSONArray("keys"), shortName);
        add(descriptor.getJSONArray("artifact"), artifact);
        add(descriptor.getJSONArray("prefixes"), prefix);
        add(descriptor.getJSONArray("examples"), example);
        
        drivers.addDescriptor(descriptor);
        
        System.err.println("Registered");
        System.out.println(descriptor.toString(4));
    }
    
    public void printDriver(String driver)
    {
        JDBCDrivers drivers = new JDBCDrivers();
        JSONObject selected = drivers.getDescriptor(driver);
        
        if(selected == null) throw new ConvirganceException("Unknown driver: " + driver);
        
        System.out.println(selected.toString(4));
    }
    
    public void printAll()
    {
        JDBCDrivers drivers = new JDBCDrivers();
        int[] widths = new int[]{ 14, 10, 8 };
        
        String example;
        String shortName;
        
        for(JSONObject descriptor : drivers)
        {
            shortName = !descriptor.getJSONArray("keys").isEmpty() ? descriptor.getJSONArray("keys").getString(0) : "";
            example = !descriptor.getJSONArray("examples").isEmpty() ? descriptor.getJSONArray("examples").getString(0) : "";
            
            if(widths[0] < descriptor.getString("name").length()) widths[0] = descriptor.getString("name").length();
            if(widths[1] < shortName.length()) widths[1] = shortName.length();
            if(widths[2] < example.length()) widths[2] = example.length();
        }
        
        System.out.print(formatWidth("Database Name", widths[0]));
        System.out.print("  ");
        System.out.print(formatWidth("Short Name", widths[1]));
        System.out.print("  ");
        System.out.println(formatWidth("Connection String Example", widths[2]));
        
        System.out.print(drawWidth('=', widths[0]));
        System.out.print("  ");
        System.out.print(drawWidth('=', widths[1]));
        System.out.print("  ");
        System.out.println(drawWidth('=', widths[2]));
            
        for(JSONObject descriptor : drivers)
        {
            shortName = !descriptor.getJSONArray("keys").isEmpty() ? descriptor.getJSONArray("keys").getString(0) : "";
            example = !descriptor.getJSONArray("examples").isEmpty() ? descriptor.getJSONArray("examples").getString(0) : "";
            
            System.out.print(formatWidth(descriptor.getString("name"), widths[0]));
            System.out.print("  ");
            System.out.print(formatWidth(shortName, widths[1]));
            System.out.print("  ");
            System.out.println(formatWidth(example, widths[2]));
        }
    }
    
}
