/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.MappingException;
import org.hibernate.type.Type;

/**
 * Defines operations common to "compiled" mappings (ie. {@code SessionFactory})
 * and "uncompiled" mappings (ie. {@code Configuration}) that are used by
 * implementors of {@code Type}.
 *
 * @see Type
 * @see org.hibernate.internal.SessionFactoryImpl
 * @see org.hibernate.cfg.Configuration
 * @author Gavin King
 *
 * @deprecated (since 6.0) Use {@link org.hibernate.type.spi.TypeConfiguration},
 * {@link org.hibernate.boot.Metadata} or {@link org.hibernate.metamodel.RuntimeMetamodels}
 * to access such information
 */
@Deprecated
public interface Mapping {
	Type getIdentifierType(String className) throws MappingException;
	String getIdentifierPropertyName(String className) throws MappingException;
	Type getReferencedPropertyType(String className, String propertyName) throws MappingException;
}
