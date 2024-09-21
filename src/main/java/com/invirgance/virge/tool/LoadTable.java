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

import com.invirgance.convirgance.CloseableIterator;
import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.dbms.*;
import com.invirgance.convirgance.input.*;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.source.FileSource;
import com.invirgance.convirgance.source.InputStreamSource;
import com.invirgance.convirgance.source.Source;
import com.invirgance.virge.Virge;

import static com.invirgance.virge.Virge.exit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 *
 * @author jbanes
 */
public class LoadTable implements Tool
{
    private Source source;
    private Input<JSONObject> input;

    private char inputDelimiter;
    private boolean truncate;
    private String tableName = "TABLE";
    
    private String jdbcURL;
    private String username;
    private String password;

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

    private boolean error(String message)
    {
        System.err.println(message);
        
        return false;
    }
    
    @Override
    public String getName()
    {
        return "sqlload";
    }

    @Override
    public String[] getHelp()
    {
        return new String[] {
            "sqlload <source> <jdbc url>",
            "    Load a table from an input source",
            "",
            "    --input <format>",
            "    -i <format>",
            "        Specify the format of the input file. Currently supported options are json, csv, tsv, pipe, delimited, and bson",
            "",
            "    --input-delimiter <delimiter>",
            "    -D <delimiter>",
            "         Set the column delimiter if the source is a delimited file (e.g. , or |)",
            "",
            "    --table-name <name>",
            "        Specifies the table to load. By default the input filename is used as the table name.",
            "",
            "    --truncate",
            "        Truncate the table prior to loading. All existing data will be lost!",
            "",
            "    --username <username>",
            "    -u <username>",
            "         The username to use when logging into the database",
            "",
            "    --password <password>",
            "    -p <password>",
            "         The password to use when logging into the database",
            "",
            "    --source <file path>",
            "    -s <file path>",
            "         Alternate method of specifying the source file",
            "",
            "    --jdbc-url <connection url>",
            "    -j <connection url>",
            "         Alternate method of specifying the JDBC connection url "
        };
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
                    
                case "--table-name":
                    tableName = args[++i];
                    break;
                    
                case "--truncate":
                    truncate = true;
                    break;
                    
                case "--jdbc-url":
                case "-j":
                    jdbcURL = args[++i];
                    break;
                    
                case "--username":
                case "-u":
                    username = args[++i];
                    break;
                    
                case "--password":
                case "-p":
                    password = args[++i];
                    break;
                    
                default:
                    
                    if(source == null)
                    {
                        source = getSource(args[i]);
                    
                        if(input == null) input = detectInput(args[i]);

                        break;
                    }
                    else if(jdbcURL == null)
                    {
                        jdbcURL = args[i];
                    }
                    else
                    {
                        exit(255, "Unknown parameter: " + args[i]);
                    }
            }
        }
        
        if(source == null) return error("No source specified!");
        if(input == null) return error("No input type specified and unable to autodetect");
        if(jdbcURL == null) return error("JDBC URL not specified!");
        
        return true;
    }

    private String normalizeColumn(String name)
    {
        if(name.equalsIgnoreCase("size")) return "\"" + name + "\"";
        
        return name;
    }
    
    private Query getInsertQuery() throws Exception
    {
        InputCursor<JSONObject> cursor = input.read(source);
        JSONObject record;
        
        StringBuffer sql = new StringBuffer("insert into ");
        int index = 0;
        
        sql.append(tableName);
        sql.append("(\n");
        
        try(CloseableIterator<JSONObject> iterator = cursor.iterator())
        {
            if(!iterator.hasNext()) return null;
            
            record = iterator.next();
        }
        
        for(String key : record.keySet())
        {
            if(index > 0) sql.append(",\n");
            
            sql.append("    ");
            sql.append(normalizeColumn(key));
            
            index++;
        }
        
        sql.append("\n) VALUES (");
        
        index = 0;
        
        for(String key : record.keySet())
        {
            if(index > 0) sql.append(",\n");
            
            sql.append("    ");
            sql.append(":");
            sql.append(key);
            
            index++;
        }
        
        sql.append(")");
        
        return new Query(sql.toString());
    }

    @Override
    public void execute() throws Exception
    {
        Query query = getInsertQuery();
        DBMS dbms = new DBMS(new DataSourceWrapper());
        
        TransactionOperation transaction;
        QueryOperation truncate = new QueryOperation(new Query("truncate table " + tableName));
        BatchOperation batch;
        
        if(query == null) Virge.exit(5, "Source provided no records to load!");
        
        batch = new BatchOperation(query, input.read(source));
        
        if(this.truncate) transaction = new TransactionOperation(truncate, batch);
        else transaction = new TransactionOperation(batch);
        
        dbms.update(transaction);
    }
    
    private class DataSourceWrapper implements DataSource
    {
        private PrintWriter log;

        public DataSourceWrapper()
        {
            log = new PrintWriter(System.out);
        }
        
        @Override
        public Connection getConnection() throws SQLException
        {
            return DriverManager.getConnection(jdbcURL, username, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException
        {
            return DriverManager.getConnection(jdbcURL, username, password);
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException
        {
            return this.log;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException
        {
//            this.log = out;
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public int getLoginTimeout() throws SQLException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException
        {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
        
    }
}
