/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

/**
 * Limited set of {@linkplain org.hibernate.metamodel.CollectionClassification}
 * used in mapping a dynamic model.
 *
 * @see org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionClassificationImpl
 * @see org.hibernate.metamodel.CollectionClassification
 *
 * @author Steve Ebersole
 */
public enum LimitedCollectionClassification {
	BAG,
	LIST,
	SET,
	MAP
}
