package org.hibernate.boot.internal;

/**
 * Limited set of {@linkplain org.hibernate.metamodel.CollectionClassification}
 * used in mapping a dynamic model.
 *
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
