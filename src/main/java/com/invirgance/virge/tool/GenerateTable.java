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
import com.invirgance.convirgance.source.FileSource;
import com.invirgance.convirgance.source.InputStreamSource;
import com.invirgance.convirgance.source.Source;
import com.invirgance.convirgance.transform.CoerceStringsTransformer;
import com.invirgance.virge.Virge;

import static com.invirgance.virge.Virge.exit;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormatSymbols;

/**
 *
 * @author jbanes
 */
public class GenerateTable implements Tool
{
    private Source source;
    private Input<JSONObject> input;

    private char inputDelimiter;
    private boolean detectTypes;
    private String tableName = "TABLE";

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
        URL url;
        
        if(isURL(path))
        {
            url = URI.create(path).toURL();
            tableName = url.getFile();
            
            if(tableName.contains(".")) tableName = tableName.substring(0, tableName.indexOf('.'));
            
            return new InputStreamSource(url.openStream());
        }
        
        file = new File(path);
        
        if(!file.exists()) throw new ConvirganceException("File not found: " + path);
        
        tableName = file.getName();
            
        if(tableName.contains(".")) tableName = tableName.substring(0, tableName.indexOf('.'));
        
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
    
    @Override
    public String getName()
    {
        return "sqltable";
    }

    @Override
    public String[] getHelp()
    {
        return new String[] {
            "sqltable <source>",
            "    Generates a SQL script to create a table based on a source data file",
            "",
            "    --input <format>",
            "    -i <format>",
            "        Specify the format of the input file. Currently supported options are json, csv, tsv, pipe, delimited, and bson",
            "",
            "    --input-delimiter <delimiter>",
            "    -D <delimiter>",
            "         Set the column delimiter if the source is a delimited file (e.g. , or |)",
            "",
            "    --source <file path>",
            "    -s <file path>",
            "         Alternate method of specifying the source file"
        };
    }

    private boolean error(String message)
    {
        System.err.println(message);
        
        return false;
    }

    @Override
    public boolean parse(String[] args, int start) throws Exception
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
                
                case "--input-delimiter":
                case "-D":
                    inputDelimiter = args[++i].charAt(0);
                    
                    if(input instanceof DelimitedInput) ((DelimitedInput)input).setDelimiter(inputDelimiter);
                    
                    break;
                    
                default:
                    
                    if(source == null)
                    {
                        source = getSource(args[i]);
                    
                        if(input == null) input = detectInput(args[i]);

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
        
        return true;
    }

    private String normalizeObjectName(String name)
    {
        return "\"" + name + "\"";
    }
    
    @Override
    public void execute() throws Exception
    {
        Iterable<JSONObject> iterable;
        StringBuffer sql = new StringBuffer();
        StringBuffer comments = new StringBuffer();
        
        Column[] columns = null;
        int index;
        
        
        if(source == null) Virge.exit(254, "No source specified!");
        if(input == null) Virge.exit(254, "No input type specified and unable to autodetect");
        
        iterable = input.read(source);
        
        if(detectTypes) iterable = new CoerceStringsTransformer().transform(iterable);
        
        for(JSONObject record : iterable)
        {
            if(columns == null) columns = new Column[record.size()];
            
            index = 0;
            
            for(String key : record.keySet())
            {
                if(columns[index] == null) columns[index] = new Column(key);
                
                columns[index++].analyze(record.get(key));
            }
        }
        
        sql.append("CREATE TABLE ");
        sql.append(tableName);
        sql.append(" (\n");
        
        index = 0;
        
        for(Column column : columns)
        {
            if(index++ > 0) sql.append(",\n");
            
            sql.append("    ");
            sql.append(normalizeObjectName(column.name));
            sql.append(" ");
            sql.append(column.getType());
            
            if(column.nullable)
            {
                sql.append(" ");
                sql.append("NULL");
            }
        }
        
        sql.append("\n);\n");
        
        System.out.println(comments);
        System.out.println(sql);
    }
    
    private class Column
    {
        String name;
        
        DecimalFormatSymbols symbols;
        
        Boolean numeric;
        Boolean decimal;
        Boolean bool;
        boolean nullable;
        
        long smallestInteger;
        long largestInteger;
        
        double smallestDouble;
        double largestDouble;
        
        int length = 8; // Minimum 8 character varchar

        public Column(String name)
        {
            this.name = name;
            this.symbols = new DecimalFormatSymbols();
        }
        
        private boolean isNumeric(Object value)
        {
            String string;
            int periods = 0;
            
            char point = symbols.getDecimalSeparator();
            char c;
            
            if(value instanceof Number) return true;
            
            if(value instanceof String)
            {
                string = value.toString();
                
                for(int i=0; i<string.length(); i++)
                {
                    c = string.charAt(i);
                    
                    if(!Character.isDigit(c) && c != point && !(c == '-' && i == 0))
                    {
                        return false;
                    }
                    
                    if(c == point) periods++;
                }
                
                return (periods <= 1);
            }
            
            return false;
        }
        
        private boolean isDecimal(Object value)
        {
            char point = symbols.getDecimalSeparator();
            
            if(!numeric) return false;
            
            if(value instanceof Float) return true;
            if(value instanceof Double) return true;
            if(value instanceof BigDecimal) return true;
            
            if(value instanceof String && value.toString().indexOf(point) > 0) return true;
            
            return false;
        }
        
        private boolean isBoolean(Object value)
        {
            if(decimal) return false;
            
            if(value instanceof Boolean) return true;
            if(numeric && value.toString().equals("1")) return true;
            if(numeric && value.toString().equals("0")) return true;
            
            if(value instanceof String)
            {
                if(value.toString().equalsIgnoreCase("true")) return true;
                if(value.toString().equalsIgnoreCase("false")) return true;
            }
            
            return false;
        }
        
        public void analyze(Object value)
        {
            long tempLong;
            double tempDouble;
            byte[] tempBytes;
            
            if(value == null || value.equals(""))
            {
                nullable = true;
                return;
            }
            
            if(numeric == null || numeric) numeric = isNumeric(value);
            if(decimal == null || !decimal) decimal = isDecimal(value);
            if(bool == null || bool) bool = isBoolean(value);
            
            if(numeric && !decimal)
            {
                tempLong = Long.parseLong(value.toString());
                
                if(tempLong < smallestInteger) smallestInteger = tempLong;
                if(tempLong > smallestInteger) largestInteger = tempLong;
            }
            
            if(numeric && decimal)
            {
                tempDouble = Double.parseDouble(value.toString());
                
                if(tempDouble < smallestDouble) smallestDouble = tempDouble;
                if(tempDouble > largestDouble) largestDouble = tempDouble;
            }
            
            tempBytes = value.toString().getBytes();
            
            if(tempBytes.length > length) length = tempBytes.length;
        }
        
        private int getPrecision()
        {
            int length = Math.max(Double.toString(smallestDouble).length(), Double.toString(largestDouble).length());
            
            return Math.max(Math.min(38, length), 12);
        }
        
        private int getScale()
        {
            long scale = (long)Math.max(Math.abs(smallestDouble), Math.abs(largestDouble));

            return Math.min(getPrecision() - Long.toString(scale).length(), 8);
        }
        
        private String getIntegerType()
        {
            if(smallestDouble <= Integer.MIN_VALUE) return "BIGINT";
            if(largestDouble >= Integer.MAX_VALUE) return "BIGINT";
            
            return "INT";
        }
        
        private String getVarchar()
        {
            if(length > 2048) return "CLOB";
            
            for(int i=8; i<=2048; i*=2)
            {
                if(i >= length) return "VARCHAR(" + i + ")";
            }
            
            return "VARCHAR";
        }
        
        public String getType()
        {
            if(bool != null && bool) return "BIT";
            if(numeric != null && numeric && decimal) return "NUMERIC(" + getPrecision() + "," + getScale() + ")";
            if(numeric != null && numeric) return getIntegerType();
            
            return getVarchar();
        }
    }
}
