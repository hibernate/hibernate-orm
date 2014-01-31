/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal.metamodel.builder;

/**
 * Represents Hibernate mapping features that are not supported in JPA metamodel.  Used to allow control over how
 * such features are handled in regards to building the JPA {@link javax.persistence.metamodel.Metamodel} instance.
 *
 * @author Steve Ebersole
*/
public enum UnsupportedFeature {
	ANY( "ANY mappings not supported in JPA metamodel" ),
	ARRAY( "Arrays (HBM <array/> mappings) are not supported in JPA metamodel" );

	private final String message;

	UnsupportedFeature(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
