/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
