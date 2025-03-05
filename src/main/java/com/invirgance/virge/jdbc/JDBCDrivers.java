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
package com.invirgance.virge.jdbc;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.source.ClasspathSource;
import com.invirgance.convirgance.storage.Config;
import static com.invirgance.virge.Virge.hideLoggingError;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.Iterator;
import javax.sql.DataSource;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * Manages the JDBCDrivers config and retrieving the drivers from Maven
 * @author jbanes
 */
public class JDBCDrivers implements Iterable<JSONObject>
{
    private final File drivers;
    private final Config config;

    public JDBCDrivers()
    {
        File home = new File(System.getProperty("user.home"));
        
        // Disable unnecessary maven logging
        hideLoggingError();
        
        this.drivers = new File(new File(new File(home, ".virge"), "database"), "drivers");
        this.config = new Config(new ClasspathSource("/database/drivers.json"), this.drivers, "name");
    }
    
    
    public JSONObject getDescriptor(String type)
    {
        for(JSONObject descriptor : this)
        {
            if(descriptor.getString("name").equalsIgnoreCase(type)) return descriptor;
            
            if(descriptor.getJSONArray("keys") == null) continue;
            
            for(String key : (JSONArray<String>)descriptor.getJSONArray("keys"))
            {
                if(key.equalsIgnoreCase(type)) return descriptor;
            }
        }
        
        return null;
    }
    
    public JSONObject findDescriptorByURL(String url)
    {
        for(JSONObject descriptor : this)
        {
            for(String prefix : (JSONArray<String>)descriptor.getJSONArray("prefixes"))
            {
                if(url.startsWith(prefix)) return descriptor;
            }
        }
        
        return null;
    }
    
    public void addDescriptor(JSONObject descriptor)
    {
        config.insert(descriptor);
    }
    
    public void deleteDescriptor(JSONObject descriptor)
    {
        config.delete(descriptor);
    }
    
    private URL[] translate(File[] files)
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
    
    public Driver getDriver(String type)
    {
        JSONObject descriptor = getDescriptor(type);
        ConfigurableMavenResolverSystem maven = Maven.configureResolver();
        
        Class clazz;
        URLClassLoader loader;
        File[] files;
        
        if(descriptor == null) return null;
        
        files =  maven.withMavenCentralRepo(true).resolve(descriptor.getJSONArray("artifact")).withTransitivity().asFile();
        loader = new URLClassLoader(translate(files));
        
        try
        {
            clazz = loader.loadClass(descriptor.getString("driver"));
            
            return (Driver)clazz.getDeclaredConstructor().newInstance();
        }
        catch(ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            throw new ConvirganceException(e);
        }
    }
    
    public Driver getDriverByURL(String url)
    {
        JSONObject descriptor = findDescriptorByURL(url);
        
        if(descriptor == null) return null;
        
        return (Driver)getDriver(descriptor.getString("name"));
    }
    
    public DataSource getDataSource(String type)
    {
        JSONObject descriptor = getDescriptor(type);
        ConfigurableMavenResolverSystem maven = Maven.configureResolver();
        
        Class clazz;
        URLClassLoader loader;
        File[] files;
        
        if(descriptor == null) return null;
        
        files =  maven.withMavenCentralRepo(true).resolve(descriptor.getJSONArray("artifact")).withTransitivity().asFile();
        loader = new URLClassLoader(translate(files));
        
        try
        {
            clazz = loader.loadClass(descriptor.getString("datasource"));
            
            return (DataSource)clazz.getDeclaredConstructor().newInstance();
        }
        catch(ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
        {
            throw new ConvirganceException(e);
        }
    }
    
    public DataSource getDataSource(String url, String username, String password)
    {
        JSONObject descriptor = findDescriptorByURL(url);
        DataSource source;
        
        if(descriptor == null) return null;
        
        source = getDataSource(descriptor.getString("name"));
        
        try
        {
            for(Method method : source.getClass().getMethods())
            {
                if(method.getName().toLowerCase().startsWith("seturl") && method.getParameterCount() == 1)
                {
                    method.invoke(source, url);
                }
                
                if(method.getName().toLowerCase().startsWith("setuser") && method.getParameterCount() == 1)
                {
                    method.invoke(source, username);
                }
                
                if(method.getName().toLowerCase().startsWith("setpass") && method.getParameterCount() == 1)
                {
                    method.invoke(source, password);
                }
            }
        }
        catch(IllegalAccessException | InvocationTargetException e)
        {
            throw new ConvirganceException(e);
        }
        
        return source;
    }

    @Override
    public Iterator<JSONObject> iterator()
    {
        return config.iterator();
    }
    
}
