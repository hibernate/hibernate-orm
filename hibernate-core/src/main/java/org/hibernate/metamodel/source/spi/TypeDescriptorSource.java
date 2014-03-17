/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

/**
 * Describes the source of a custom type description.  For example, {@code <type-def/>} or
 * {@link org.hibernate.annotations.TypeDef @TypeDefinition}
 *
 * @author Steve Ebersole
 */
public interface TypeDescriptorSource {
	/**
	 * Retrieve the name of the type def.
	 *
	 * @return The name.
	 */
	String getName();

	/**
	 * Retrieve the name of the class implementing {@link org.hibernate.type.Type},
	 * {@link org.hibernate.usertype.UserType}, etc.
	 *
	 * @return The implementation class name.
	 */
	String getTypeImplementationClassName();

	/**
	 * For what are termed "basic types" there is a registry that contain the type keyed by various
	 * keys.  This is the mechanism that allows a "string type" to reference to by "string", "java.lang.String",
	 * etc in the mapping.  This method returns the keys under which this type should be registered in
	 * that registry.
	 * <p/>
	 * Note that if the type def contains registration keys, it should be considered illegal for its
	 * corresponding {@link HibernateTypeSource} to define parameters.
	 *
	 * @return The registration keys for the type built from this type def.
	 */
	String[] getRegistrationKeys();

	/**
	 * Types accept configuration.  The values here represent the user supplied values that will be given
	 * to the type instance after instantiation
	 *
	 * @return The configuration parameters from the underlying source.
	 */
	Map<String,String> getParameters();
}
