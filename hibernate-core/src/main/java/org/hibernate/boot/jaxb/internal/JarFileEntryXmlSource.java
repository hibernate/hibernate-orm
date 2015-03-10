/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
