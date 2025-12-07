package org.zerwhit.core.obfuscation;

import LZMA.LzmaInputStream;
import com.google.common.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;

public class LZMAInputSupplier extends ByteSource {
    private InputStream compressedData;

    public LZMAInputSupplier(InputStream compressedData)
    {
        this.compressedData = compressedData;
    }

    @Override
    public InputStream openStream() throws IOException
    {
        return new LzmaInputStream(this.compressedData);
    }

}