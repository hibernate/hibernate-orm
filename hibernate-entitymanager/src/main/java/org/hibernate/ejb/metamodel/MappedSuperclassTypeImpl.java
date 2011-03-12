package org.hibernate.ejb.metamodel;

import javax.persistence.metamodel.MappedSuperclassType;
import javax.persistence.metamodel.Type;

/**
 * @author Emmanuel Bernard
 */
public class MappedSuperclassTypeImpl<X> extends AbstractIdentifiableType<X> implements MappedSuperclassType<X> {
	public MappedSuperclassTypeImpl(
			Class<X> javaType,
			AbstractIdentifiableType<? super X> superType,
			boolean hasIdentifierProperty,
			boolean versioned) {
		super( javaType, superType, hasIdentifierProperty, versioned );
	}

	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}
}
