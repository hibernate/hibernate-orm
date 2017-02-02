/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.spi;

import java.io.InputStream;
import javax.xml.transform.Source;

import org.hibernate.boot.jaxb.Origin;

/**
 * Contract for performing JAXB binding.
 *
 * @author Steve Ebersole
 */
public interface Binder {
	/**
	 * Bind from an XML source.
	 *
	 * @param source The XML source.
	 * @param origin The descriptor of the source origin
	 *
	 * @return The bound JAXB model
	 */
	public Binding bind(Source source, Origin origin);

	/**
	 * Bind from an InputStream
	 *
	 * @param stream The InputStream containing XML
	 * @param origin The descriptor of the stream origin
	 *
	 * @return The bound JAXB model
	 */
	public Binding bind(InputStream stream, Origin origin);
}
