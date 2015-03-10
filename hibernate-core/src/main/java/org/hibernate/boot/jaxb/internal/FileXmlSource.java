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
	public Binding doBind(Binder binder) {
		return doBind( binder, file, getOrigin() );
	}

	public static Binding doBind(Binder binder, File file, Origin origin) {
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
