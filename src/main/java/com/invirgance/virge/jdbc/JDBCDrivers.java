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
package com.invirgance.virge.jdbc;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.input.JSONInput;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.source.ClasspathSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.util.Iterator;
import javax.sql.DataSource;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 *
 * @author jbanes
 */
public class JDBCDrivers implements Iterable<JSONObject>
{
    public JSONObject getDescriptor(String type)
    {
        for(JSONObject descriptor : this)
        {
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
    
    public Driver getDriver(String type)
    {
        JSONObject descriptor = getDescriptor(type);
        ConfigurableMavenResolverSystem maven = Maven.configureResolver();
        
        Class clazz;
        URLClassLoader loader;
        URL[] urls;
        
        if(descriptor == null) return null;
        
        urls =  maven.withMavenCentralRepo(true).resolve(descriptor.getString("artifact")).withTransitivity().as(URL.class);
        loader = new URLClassLoader(urls);
        
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
        
        return (Driver)getDriver(descriptor.getJSONArray("keys").getString(0));
    }
    
    public DataSource getDataSource(String type)
    {
        JSONObject descriptor = getDescriptor(type);
        ConfigurableMavenResolverSystem maven = Maven.configureResolver();
        
        Class clazz;
        URLClassLoader loader;
        URL[] urls;
        
        if(descriptor == null) return null;
        
        urls =  maven.withMavenCentralRepo(true).resolve(descriptor.getString("artifact")).withTransitivity().as(URL.class);
        loader = new URLClassLoader(urls);
        
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
        
        source = getDataSource(descriptor.getJSONArray("keys").getString(0));
        
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
        ClasspathSource source = new ClasspathSource("/database/drivers.json");
        
        return new JSONInput().read(source).iterator();
    }
    
    
}
