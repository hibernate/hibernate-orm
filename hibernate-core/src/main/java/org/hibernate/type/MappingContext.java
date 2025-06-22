/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.MappingException;

/**
 * Declares operations used by implementors of {@link Type} that are common to the fully-"compiled"
 * runtime mapping metadata held by a {@link org.hibernate.SessionFactory} and the incomplete metamodel
 * which exists during the {@linkplain org.hibernate.boot.model.process.spi.MetadataBuildingProcess
 * metadata building process}.
 *
 * @see Type
 * @see org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor
 * @see org.hibernate.boot.Metadata
 *
 * @since 7.0
 */
public interface MappingContext {
	Type getIdentifierType(String className) throws MappingException;

	String getIdentifierPropertyName(String className) throws MappingException;

	Type getReferencedPropertyType(String className, String propertyName) throws MappingException;
}
