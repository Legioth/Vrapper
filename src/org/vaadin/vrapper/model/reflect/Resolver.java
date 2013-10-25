package org.vaadin.vrapper.model.reflect;

import java.io.IOException;
import java.io.InputStream;

public interface Resolver {
    public InputStream findClassStream(String name) throws IOException;

    public void close() throws IOException;
}
