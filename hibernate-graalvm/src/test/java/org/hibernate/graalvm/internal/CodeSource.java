/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graalvm.internal;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

class CodeSource implements Closeable {

	public static CodeSource open(URI location) throws IOException {
		if ( "jar".equals( location.getScheme() ) ) {
			var fs = FileSystems.newFileSystem( location, Map.of() );
			return new CodeSource( fs, fs.getRootDirectories().iterator().next() );
		}
		else if ( "file".equals( location.getScheme() ) && location.getPath().endsWith( ".jar" ) ) {
			location = URI.create( "jar:" + location );
			var fs = FileSystems.newFileSystem( location, Map.of() );
			return new CodeSource( fs, fs.getRootDirectories().iterator().next() );
		}
		else if ( "file".equals( location.getScheme() ) ) {
			return new CodeSource( null, Paths.get( location ) );
		}
		else {
			throw new IllegalArgumentException( "Unsupported URI: " + location );
		}
	}

	private final FileSystem toClose;
	private final Path root;

	private CodeSource(FileSystem toClose, Path root) {
		this.toClose = toClose;
		this.root = root;
	}

	@Override
	public void close() throws IOException {
		if ( toClose != null ) {
			toClose.close();
		}
	}

	public Path getRoot() {
		return root;
	}
}
