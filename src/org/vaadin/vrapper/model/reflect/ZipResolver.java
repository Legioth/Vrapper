package org.vaadin.vrapper.model.reflect;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipResolver implements Resolver {

    private final ZipFile file;

    public ZipResolver(ZipFile file) {
        this.file = file;
    }

    @Override
    public InputStream findClassStream(String name) throws IOException {
        ZipEntry entry = file.getEntry(name + ".class");
        if (entry == null) {
            return null;
        } else {
            return file.getInputStream(entry);
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

}
