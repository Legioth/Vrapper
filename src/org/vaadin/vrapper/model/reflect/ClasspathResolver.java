package org.vaadin.vrapper.model.reflect;

import java.io.IOException;
import java.io.InputStream;

public class ClasspathResolver implements Resolver {

    @Override
    public InputStream findClassStream(String name) throws IOException {
        return getClass().getResourceAsStream('/' + name + ".class");
    }

    @Override
    public void close() throws IOException {
        // Nothing to do
    }

}
