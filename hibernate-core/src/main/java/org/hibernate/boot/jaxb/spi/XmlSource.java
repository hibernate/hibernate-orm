/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.spi;

import org.hibernate.boot.jaxb.Origin;

/**
 * An XML document containing O/R mapping metadata, either:
 * <ul>
 *     <li>a JPA {@code orm.xml} file, or
 *     <li>a Hibernate {@code .hbm.xml} file.
 * </ul>
 *
 * @author Steve Ebersole
 */
public abstract class XmlSource {
	private final Origin origin;

	protected XmlSource(Origin origin) {
		this.origin = origin;
	}

	public Origin getOrigin() {
		return origin;
	}

	public abstract <T> Binding<T> doBind(Binder<T> binder);
}
