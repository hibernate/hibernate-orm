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
package org.hibernate.metamodel.source.spi;

import java.util.Map;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;

/**
 * Source-agnostic descriptor for explicit user-supplied Hibernate type information
 *
 * @author Steve Ebersole
 */
public interface HibernateTypeSource {
	/**
	 * Obtain the supplied Hibernate type name.
	 *
	 * @return The Hibernate type name
	 */
	String getName();

	/**
	 * Obtain any supplied Hibernate type parameters.
	 *
	 * @return The Hibernate type parameters.
	 */
	Map<String,String> getParameters();

	/**
	 * Obtain the attribute's java type if possible.
	 *
	 * @return The java type of the attribute or null.
	 */
	JavaTypeDescriptor getJavaType();
}
