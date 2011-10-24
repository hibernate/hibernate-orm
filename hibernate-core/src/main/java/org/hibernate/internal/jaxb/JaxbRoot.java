/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal.jaxb;

/**
 * Holds information about a JAXB-unmarshalled XML document.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class JaxbRoot<T> {
	private final T root;
	private final Origin origin;

	public JaxbRoot(T root, Origin origin) {
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
