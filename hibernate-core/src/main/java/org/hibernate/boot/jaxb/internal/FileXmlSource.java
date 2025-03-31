/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;

/**
 * @author Steve Ebersole
 */
public class FileXmlSource extends XmlSource {
	private final File file;

	public FileXmlSource(Origin origin, File file) {
		super( origin );
		this.file = file;
	}

	@Override
	public <T> Binding<T> doBind(Binder<T> binder) {
		return doBind( binder, file, getOrigin() );
	}

	public static <T> Binding<T> doBind(Binder<T> binder, File file, Origin origin) {
		final FileInputStream fis;
		try {
			fis = new FileInputStream( file );
		}
		catch ( FileNotFoundException e ) {
			throw new MappingNotFoundException( e, origin );
		}
		return InputStreamXmlSource.doBind( binder, fis, origin, true );
	}
}
