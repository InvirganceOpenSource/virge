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

/**
 *
 * @author jbanes
 */
public class SQLDrivers implements Tool
{
    private String driver;

    @Override
    public String getName()
    {
        return "sqldrivers";
    }

    @Override
    public String[] getHelp()
    {
        return new String[] {
            "sqldrivers",
            "    Lists available jdbc drivers for connecting to databases",
            "",
            "    --driver <driver>",
            "    -d <driver>",
            "         Print additional info about the specified driver "
        };
    }

    @Override
    public boolean parse(String[] args, int start) throws Exception
    {
        for(int i=start; i<args.length; i++)
        {
            switch(args[i])
            {
                case "--driver":
                case "-d":
                    this.driver = args[++i];
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
        if(driver != null) printDriver(driver);
        else printAll();
    }
    
    public void printDriver(String driver)
    {
        JDBCDrivers drivers = new JDBCDrivers();
        JSONObject selected = null;
        
        for(JSONObject descriptor : drivers)
        {
            if(descriptor.getString("name").equalsIgnoreCase(driver))
            {
                selected = descriptor;
            }
            
            for(String key : (JSONArray<String>)descriptor.getJSONArray("keys"))
            {
                if(key.equalsIgnoreCase(driver))
                {
                    selected = descriptor;
                }
            }
        }
        
        if(selected == null) throw new ConvirganceException("Unknown driver: " + driver);
        
        System.out.println(selected.toString(4));
    }
    
    public void printAll()
    {
        JDBCDrivers drivers = new JDBCDrivers();
        int[] widths = new int[]{ 14, 10, 8 };
        
        for(JSONObject descriptor : drivers)
        {
            if(widths[0] < descriptor.getString("name").length()) widths[0] = descriptor.getString("name").length();
            if(widths[1] < descriptor.getJSONArray("keys").getString(0).length()) widths[1] = descriptor.getJSONArray("keys").getString(0).length();
            if(widths[2] < descriptor.getJSONArray("examples").getString(0).length()) widths[2] = descriptor.getJSONArray("examples").getString(0).length();
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
            System.out.print(formatWidth(descriptor.getString("name"), widths[0]));
            System.out.print("  ");
            System.out.print(formatWidth(descriptor.getJSONArray("keys").getString(0), widths[1]));
            System.out.print("  ");
            System.out.println(formatWidth(descriptor.getJSONArray("examples").getString(0), widths[2]));
        }
    }
    
}
