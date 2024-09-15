/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.MappingException;

/**
 * Declares operations used by implementors of {@link Type} that are common to
 * "compiled" mappings held at runtime by a {@link org.hibernate.SessionFactory}
 * and "uncompiled" mappings held by a {@link org.hibernate.cfg.Configuration}.
 *
 * @see Type
 * @see org.hibernate.internal.SessionFactoryImpl
 * @see org.hibernate.cfg.Configuration
 *
 *
 */
public interface MappingContext {
	Type getIdentifierType(String className) throws MappingException;

	String getIdentifierPropertyName(String className) throws MappingException;

	Type getReferencedPropertyType(String className, String propertyName) throws MappingException;
}
