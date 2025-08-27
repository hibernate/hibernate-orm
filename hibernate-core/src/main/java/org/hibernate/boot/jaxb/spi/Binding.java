/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.spi;

import java.io.Serializable;

import org.hibernate.boot.jaxb.Origin;

/**
 * Represents a JAXB binding, as well as keeping information about the origin
 * of the processed XML
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Binding<T> implements Serializable {
	private final T root;
	private final Origin origin;

	public Binding(T root, Origin origin) {
		this.root = root;
		this.origin = origin;
	}

	/**
	 * Obtain the root JAXB bound object
	 *
	 * @return The JAXB root object
	 */
	public T getRoot() {
		return root;
	}

	/**
	 * Obtain the metadata about the document's origin
	 *
	 * @return The origin
	 */
	public Origin getOrigin() {
		return origin;
	}
}
