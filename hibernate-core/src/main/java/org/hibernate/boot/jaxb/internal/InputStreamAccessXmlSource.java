/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;

/**
 * @author Steve Ebersole
 */
public class InputStreamAccessXmlSource extends XmlSource {
	private final InputStreamAccess inputStreamAccess;

	public InputStreamAccessXmlSource(Origin origin, InputStreamAccess inputStreamAccess) {
		super( origin );
		this.inputStreamAccess = inputStreamAccess;
	}

	@Override
	public Binding doBind(Binder binder) {
		return doBind( binder, inputStreamAccess, getOrigin() );
	}

	public static Binding doBind(Binder binder, InputStreamAccess inputStreamAccess, Origin origin) {
		return inputStreamAccess.fromStream(
				inputStream -> binder.bind( inputStream, origin )
		);
	}
}
