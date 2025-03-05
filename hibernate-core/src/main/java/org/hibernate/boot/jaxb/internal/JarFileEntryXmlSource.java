/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;

/**
 * @author Steve Ebersole
 */
public class JarFileEntryXmlSource extends XmlSource {
	private final JarFile jarFile;
	private final ZipEntry jarFileEntry;

	public JarFileEntryXmlSource(
			Origin origin,
			JarFile jarFile,
			ZipEntry jarFileEntry) {
		super( origin );
		this.jarFile = jarFile;
		this.jarFileEntry = jarFileEntry;
	}

	@Override
	public Binding doBind(Binder binder) {
		final InputStream stream;
		try {
			stream = jarFile.getInputStream( jarFileEntry );
		}
		catch (IOException e) {
			throw new MappingException(
					String.format(
							"Unable to open InputStream for jar file entry [%s : %s]",
							jarFile.getName(),
							jarFileEntry.getName()
					),
					e,
					getOrigin()
			);
		}

		return InputStreamXmlSource.doBind( binder, stream, getOrigin(), true );
	}
}
