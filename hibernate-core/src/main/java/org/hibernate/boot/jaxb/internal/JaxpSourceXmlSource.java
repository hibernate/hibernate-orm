/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.internal;

import javax.xml.transform.Source;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.spi.Binder;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.XmlSource;

/**
 * @author Steve Ebersole
 */
public class JaxpSourceXmlSource extends XmlSource {
	private final Source jaxpSource;

	public JaxpSourceXmlSource(Origin origin, Source jaxpSource) {
		super( origin );
		this.jaxpSource = jaxpSource;
	}

	@Override
	public Binding doBind(Binder binder) {
		return binder.bind( jaxpSource, getOrigin() );
	}
}
