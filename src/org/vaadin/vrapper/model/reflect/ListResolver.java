package org.vaadin.vrapper.model.reflect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class ListResolver implements Resolver {

	private final List<Resolver> resolvers;

	public ListResolver(List<Resolver> resolvers) {
		if (resolvers == null) {
			throw new IllegalArgumentException("resolvers can not be null");
		}
		this.resolvers = resolvers;
	}

	public ListResolver(Resolver... resolvers) {
		if (resolvers == null) {
			throw new IllegalArgumentException("resolvers can not be null");
		}
		this.resolvers = Arrays.asList(resolvers);
	}

	@Override
	public InputStream findClassStream(String name) throws IOException {
		for (Resolver resolver : resolvers) {
			InputStream stream = resolver.findClassStream(name);
			if (stream != null) {
				return stream;
			}
		}
		return null;
	}

	@Override
	public void close() throws IOException {
		IOException firstException = null;
		int ignoreCount = 0;
		for (Resolver resolver : resolvers) {
			try {
				resolver.close();
			} catch (IOException e) {
				if (firstException == null) {
					firstException = e;
				} else {
					ignoreCount++;
				}
			}
		}

		if (firstException != null) {
			throw new IOException("Closing resolvers failed," + ignoreCount
					+ " exceptions ignored", firstException);
		}
	}

}
