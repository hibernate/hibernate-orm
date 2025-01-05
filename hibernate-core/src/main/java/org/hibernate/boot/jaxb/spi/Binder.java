/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.spi;

import javax.xml.transform.Source;

import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.boot.jaxb.Origin;

/**
 * Contract for performing JAXB binding.
 *
 * @author Steve Ebersole
 */
public interface Binder<T> {
	/**
	 * Bind from an XML source.
	 *
	 * @param source The XML source.
	 * @param origin The descriptor of the source origin
	 * @return The bound JAXB model
	 */
	<X extends T> Binding<X> bind(Source source, Origin origin);

	/**
	 * Bind from an InputStreamAccess
	 *
	 * @param streamAccess The {@link InputStreamAccess} providing access to the stream containing XML
	 * @param origin The descriptor of the stream origin
	 * @return The bound JAXB model
	 */
	<X extends T> Binding<X> bind(InputStreamAccess streamAccess, Origin origin);
}
