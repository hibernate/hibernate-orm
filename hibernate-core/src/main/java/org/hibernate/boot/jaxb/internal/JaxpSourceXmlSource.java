/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
