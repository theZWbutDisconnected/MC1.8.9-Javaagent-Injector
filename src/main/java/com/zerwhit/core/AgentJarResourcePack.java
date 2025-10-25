package com.zerwhit.core;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

public class AgentJarResourcePack implements IResourcePack {
    private final File agentJarFile;
    private final Set<String> resourceDomains;

    public AgentJarResourcePack(File agentJarFile) {
        this.agentJarFile = agentJarFile;
        this.resourceDomains = Collections.singleton("zerwhit");
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!resourceExists(location)) {
            throw new IOException("Resource not found: " + location);
        }
        
        String resourcePath = "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();

        JarFile jarFile = new JarFile(agentJarFile);
        JarEntry entry = jarFile.getJarEntry(resourcePath);
        
        if (entry == null) {
            jarFile.close();
            throw new IOException("Resource not found in agent JAR: " + resourcePath);
        }
        
        InputStream is = jarFile.getInputStream(entry);
        return new InputStreamWrapper(is, jarFile);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!"zerwhit".equals(location.getResourceDomain())) {
            return false;
        }
        
        String resourcePath = "assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
        
        try (JarFile jarFile = new JarFile(agentJarFile)) {
            JarEntry entry = jarFile.getJarEntry(resourcePath);
            return entry != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Set<String> getResourceDomains() {
        return resourceDomains;
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
        return null;
    }

    @Override
    public String getPackName() {
        return "AgentJarResources";
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }
    
    // 包装类，用于在关闭InputStream时同时关闭JarFile
    private static class InputStreamWrapper extends InputStream {
        private final InputStream wrapped;
        private final JarFile jarFile;
        
        public InputStreamWrapper(InputStream wrapped, JarFile jarFile) {
            this.wrapped = wrapped;
            this.jarFile = jarFile;
        }
        
        @Override
        public int read() throws IOException {
            return wrapped.read();
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            return wrapped.read(b);
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return wrapped.read(b, off, len);
        }
        
        @Override
        public void close() throws IOException {
            try {
                wrapped.close();
            } finally {
                jarFile.close();
            }
        }
        
        @Override
        public int available() throws IOException {
            return wrapped.available();
        }
        
        @Override
        public synchronized void mark(int readlimit) {
            wrapped.mark(readlimit);
        }
        
        @Override
        public synchronized void reset() throws IOException {
            wrapped.reset();
        }
        
        @Override
        public boolean markSupported() {
            return wrapped.markSupported();
        }
    }
}