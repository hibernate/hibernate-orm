/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
