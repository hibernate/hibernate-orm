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
package org.hibernate.metamodel.domain;

import org.hibernate.internal.util.ValueHolder;

/**
 * Basic information about a Java type, in regards to its role in particular set of mappings.
 *
 * @author Steve Ebersole
 */
public interface Type {
	/**
	 * Obtain the name of the type.
	 *
	 * @return The name
	 */
	public String getName();

	/**
	 * Obtain the java class name for this type.
	 *
	 * @return The class name
	 */
	public String getClassName();

	/**
	 * Obtain the java {@link Class} reference for this type
	 *
	 * @return The {@link Class} reference
	 *
	 * @throws org.hibernate.service.classloading.spi.ClassLoadingException Indicates the class reference
	 * could not be determined.  Generally this is the case in reverse-engineering scenarios where the specified
	 * domain model classes do not yet exist.
	 */
	public Class<?> getClassReference();

	public ValueHolder<Class<?>> getClassReferenceUnresolved();

	public boolean isAssociation();

	public boolean isComponent();
}
