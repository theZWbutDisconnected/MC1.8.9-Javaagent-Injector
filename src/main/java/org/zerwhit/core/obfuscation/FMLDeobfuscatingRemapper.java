/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     cpw - implementation
 *     ZerWhit - modified
 */

package org.zerwhit.core.obfuscation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.zerwhit.core.util.SafeLogger;
import org.apache.logging.log4j.Marker;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.zerwhit.core.obfuscation.ClassHierarchyResolver;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.zerwhit.core.ClassTransformer;

public class FMLDeobfuscatingRemapper extends Remapper {
    public static final FMLDeobfuscatingRemapper INSTANCE = new FMLDeobfuscatingRemapper();
    private static final SafeLogger logger = SafeLogger.getLogger(FMLDeobfuscatingRemapper.class);

    private BiMap<String, String> classNameBiMap;

    private Map<String,Map<String,String>> rawFieldMaps;
    private Map<String,Map<String,String>> rawMethodMaps;

    private Map<String,Map<String,String>> fieldNameMaps;
    private Map<String,Map<String,String>> methodNameMaps;

    private ClassLoader classLoader;

    private static final boolean DEBUG_REMAPPING = Boolean.parseBoolean(System.getProperty("fml.remappingDebug", "false"));
    private static final boolean DUMP_FIELD_MAPS = Boolean.parseBoolean(System.getProperty("fml.remappingDebug.dumpFieldMaps", "false")) && DEBUG_REMAPPING;
    private static final boolean DUMP_METHOD_MAPS = Boolean.parseBoolean(System.getProperty("fml.remappingDebug.dumpMethodMaps", "false")) && DEBUG_REMAPPING;

    private FMLDeobfuscatingRemapper()
    {
        classNameBiMap=ImmutableBiMap.of();
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        ClassHierarchyResolver.setClassLoader(classLoader);
    }

    public void setupLoadOnly(String deobfFileName, boolean loadAll)
    {
        try
        {
            InputStream mapData = getClass().getResourceAsStream(deobfFileName);
            if (mapData == null) {
                logger.error("Could not find deobfuscation map data at {}", deobfFileName);
                mapData = getClass().getClassLoader().getResourceAsStream(deobfFileName);
            }
            
            if (mapData != null) {
                logger.info("Loading deobfuscation map data from resource: {}", deobfFileName);
                StringBuilder content = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(mapData, Charsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append('\n');
                    }
                }
                CharSource srgSource = CharSource.wrap(content.toString());
                List<String> srgList = srgSource.readLines();
                rawMethodMaps = Maps.newHashMap();
                rawFieldMaps = Maps.newHashMap();
                Builder<String, String> builder = ImmutableBiMap.<String,String>builder();
                Splitter splitter = Splitter.on(CharMatcher.anyOf(": ")).omitEmptyStrings().trimResults();
                for (String line : srgList)
                {
                    String[] parts = Iterables.toArray(splitter.split(line),String.class);
                    String typ = parts[0];
                    if ("CL".equals(typ))
                    {
                        parseClass(builder, parts);
                    }
                    else if ("MD".equals(typ) && loadAll)
                    {
                        parseMethod(parts);
                    }
                    else if ("FD".equals(typ) && loadAll)
                    {
                        parseField(parts);
                    }
                }
                classNameBiMap = builder.build();
            } else {
                logger.error("Failed to load deobfuscation map data from {}", deobfFileName);
                rawMethodMaps = Maps.newHashMap();
                rawFieldMaps = Maps.newHashMap();
            }
        }
        catch (IOException ioe)
        {
            logger.error("An error occurred loading the deobfuscation map data", ioe);
            rawMethodMaps = Maps.newHashMap();
            rawFieldMaps = Maps.newHashMap();
        }
        
        if (rawMethodMaps == null) {
            rawMethodMaps = Maps.newHashMap();
        }
        if (rawFieldMaps == null) {
            rawFieldMaps = Maps.newHashMap();
        }
        
        methodNameMaps = Maps.newHashMapWithExpectedSize(rawMethodMaps.size());
        fieldNameMaps = Maps.newHashMapWithExpectedSize(rawFieldMaps.size());

    }

    public boolean isRemappedClass(String className)
    {
        return !map(className).equals(className);
    }

    private void parseField(String[] parts)
    {
        String oldSrg = parts[1];
        int lastOld = oldSrg.lastIndexOf('/');
        String cl = oldSrg.substring(0,lastOld);
        String oldName = oldSrg.substring(lastOld+1);
        String newSrg = parts[2];
        int lastNew = newSrg.lastIndexOf('/');
        String newName = newSrg.substring(lastNew+1);
        if (!rawFieldMaps.containsKey(cl))
        {
            rawFieldMaps.put(cl, Maps.<String,String>newHashMap());
        }
        String fieldType = getFieldType(cl, oldName);
        // We might be in mcp named land, where in fact the name is "new"
        if (fieldType == null) fieldType = getFieldType(cl, newName);
        rawFieldMaps.get(cl).put(oldName + ":" + fieldType, newName);
        rawFieldMaps.get(cl).put(oldName + ":null", newName);
    }

    /*
     * Cache the field descriptions for classes so we don't repeatedly reload the same data again and again
     */
    private Map<String,Map<String,String>> fieldDescriptions = Maps.newHashMap();

    // Cache null values so we don't waste time trying to recompute classes with no field or method maps
    private Set<String> negativeCacheMethods = Sets.newHashSet();
    private Set<String> negativeCacheFields = Sets.newHashSet();

    private String getFieldType(String owner, String name)
    {
        if (fieldDescriptions.containsKey(owner))
        {
            return fieldDescriptions.get(owner).get(name);
        }
        synchronized (fieldDescriptions)
        {
            try
            {
                String classResource = owner + ".class";
                InputStream classStream = null;
                
                if (classLoader != null) {
                    classStream = classLoader.getResourceAsStream(classResource);
                }
                if (classStream == null) {
                    classStream = getClass().getClassLoader().getResourceAsStream(classResource);
                }
                if (classStream == null) {
                    classStream = ClassLoader.getSystemResourceAsStream(classResource);
                }
                
                if (classStream == null)
                {
                    return null;
                }
                
                ClassReader cr = new ClassReader(classStream);
                ClassNode classNode = new ClassNode();
                cr.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                Map<String,String> resMap = Maps.newHashMap();
                for (FieldNode fieldNode : (List<FieldNode>) classNode.fields) {
                    resMap.put(fieldNode.name, fieldNode.desc);
                }
                fieldDescriptions.put(owner, resMap);
                return resMap.get(name);
            }
            catch (IOException e)
            {
                logger.error("A critical exception occured reading a class file {}", owner, e);
            }
            return null;
        }
    }

    private void parseClass(Builder<String, String> builder, String[] parts)
    {
        builder.put(parts[1],parts[2]);
    }

    private void parseMethod(String[] parts)
    {
        String oldSrg = parts[1];
        int lastOld = oldSrg.lastIndexOf('/');
        String cl = oldSrg.substring(0,lastOld);
        String oldName = oldSrg.substring(lastOld+1);
        String sig = parts[2];
        String newSrg = parts[3];
        int lastNew = newSrg.lastIndexOf('/');
        String newName = newSrg.substring(lastNew+1);
        if (!rawMethodMaps.containsKey(cl))
        {
            rawMethodMaps.put(cl, Maps.<String,String>newHashMap());
        }
        rawMethodMaps.get(cl).put(oldName+sig, newName);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc)
    {
        if (classNameBiMap == null || classNameBiMap.isEmpty())
        {
            return name;
        }
        Map<String, String> fieldMap = getFieldMap(owner);
        return fieldMap!=null && fieldMap.containsKey(name+":"+desc) ? fieldMap.get(name+":"+desc) : name;
    }

    @Override
    public String map(String typeName)
    {
        if (classNameBiMap == null || classNameBiMap.isEmpty())
        {
            return typeName;
        }
        if (classNameBiMap.containsKey(typeName))
        {
            return classNameBiMap.get(typeName);
        }
        int dollarIdx = typeName.lastIndexOf('$');
        if (dollarIdx > -1)
        {
            return map(typeName.substring(0, dollarIdx)) + "$" + typeName.substring(dollarIdx + 1);
        }
        return typeName;
    }

    public String unmap(String typeName)
    {
        if (classNameBiMap == null || classNameBiMap.isEmpty())
        {
            return typeName;
        }

        if (classNameBiMap.containsValue(typeName))
        {
            return classNameBiMap.inverse().get(typeName);
        }
        int dollarIdx = typeName.lastIndexOf('$');
        if (dollarIdx > -1)
        {
            return unmap(typeName.substring(0, dollarIdx)) + "$" + typeName.substring(dollarIdx + 1);
        }
        return typeName;
    }


    @Override
    public String mapMethodName(String owner, String name, String desc)
    {
        if (classNameBiMap==null || classNameBiMap.isEmpty())
        {
            return name;
        }
        Map<String, String> methodMap = getMethodMap(owner);
        String methodDescriptor = name+desc;
        return methodMap!=null && methodMap.containsKey(methodDescriptor) ? methodMap.get(methodDescriptor) : name;
    }

    private Map<String,String> getFieldMap(String className)
    {
        if (!fieldNameMaps.containsKey(className) && !negativeCacheFields.contains(className))
        {
            findAndMergeSuperMaps(className);
            if (!fieldNameMaps.containsKey(className))
            {
                negativeCacheFields.add(className);
            }

            if (DUMP_FIELD_MAPS)
            {
                logger.info("Field map for {} : {}", className, fieldNameMaps.get(className));
            }
        }
        return fieldNameMaps.get(className);
    }

    private Map<String,String> getMethodMap(String className)
    {
        if (!methodNameMaps.containsKey(className) && !negativeCacheMethods.contains(className))
        {
            findAndMergeSuperMaps(className);
            if (!methodNameMaps.containsKey(className))
            {
                negativeCacheMethods.add(className);
            }
            if (DUMP_METHOD_MAPS)
            {
                logger.info("Method map for {} : {}", className, methodNameMaps.get(className));
            }

        }
        return methodNameMaps.get(className);
    }

    private void findAndMergeSuperMaps(String name)
    {
        try
        {
            logger.debug("Finding super maps for class: {}", name);
            
            // Try to get super class and interfaces, but handle cases where the class doesn't exist
            String superName = null;
            String[] interfaces = null;
            
            try {
                superName = ClassHierarchyResolver.getSuperClass(name);
                interfaces = ClassHierarchyResolver.getInterfaces(name);
            } catch (IllegalArgumentException e) {
                // If the class format is invalid, it might be an obfuscated class that doesn't exist in classpath
                logger.warn("Cannot resolve class hierarchy for potentially obfuscated class: {}", name);
                superName = null;
                interfaces = new String[0];
            }
            
            mergeSuperMaps(name, superName, interfaces);
        }
        catch (Exception e)
        {
            logger.error("Error finding and merging super maps for class: {}", name, e);
        }
    }
    public void mergeSuperMaps(String name, String superName, String[] interfaces)
    {
//        System.out.printf("Computing super maps for {}: {} {}\n", name, superName, Arrays.asList(interfaces));
        if (classNameBiMap == null || classNameBiMap.isEmpty())
        {
            return;
        }
        // Skip Object
        if (Strings.isNullOrEmpty(superName))
        {
            return;
        }

        List<String> allParents = ImmutableList.<String>builder().add(superName).addAll(Arrays.asList(interfaces)).build();
        // generate maps for all parent objects
        for (String parentThing : allParents)
        {
            if (!methodNameMaps.containsKey(parentThing))
            {
                findAndMergeSuperMaps(parentThing);
            }
        }
        Map<String, String> methodMap = Maps.<String,String>newHashMap();
        Map<String, String> fieldMap = Maps.<String,String>newHashMap();
        for (String parentThing : allParents)
        {
            if (methodNameMaps.containsKey(parentThing))
            {
                methodMap.putAll(methodNameMaps.get(parentThing));
            }
            if (fieldNameMaps.containsKey(parentThing))
            {
                fieldMap.putAll(fieldNameMaps.get(parentThing));
            }
        }
        if (rawMethodMaps.containsKey(name))
        {
            methodMap.putAll(rawMethodMaps.get(name));
        }
        if (rawFieldMaps.containsKey(name))
        {
            fieldMap.putAll(rawFieldMaps.get(name));
        }
        methodNameMaps.put(name, ImmutableMap.copyOf(methodMap));
        fieldNameMaps.put(name, ImmutableMap.copyOf(fieldMap));
//        System.out.printf("Maps: {} {}\n", name, methodMap);
    }

    public Set<String> getObfedClasses()
    {
        return ImmutableSet.copyOf(classNameBiMap.keySet());
    }

    public String getStaticFieldType(String oldType, String oldName, String newType, String newName)
    {
        String fType = getFieldType(oldType, oldName);
        if (oldType.equals(newType))
        {
            return fType;
        }
        Map<String,String> newClassMap = fieldDescriptions.get(newType);
        if (newClassMap == null)
        {
            newClassMap = Maps.newHashMap();
            fieldDescriptions.put(newType, newClassMap);
        }
        newClassMap.put(newName, fType);
        return fType;
    }
}