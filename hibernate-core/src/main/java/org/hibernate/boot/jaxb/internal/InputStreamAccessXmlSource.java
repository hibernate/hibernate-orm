/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		return binder.bind( inputStreamAccess, origin ) ;
	}
}
