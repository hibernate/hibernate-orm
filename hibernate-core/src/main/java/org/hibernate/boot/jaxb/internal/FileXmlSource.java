/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
