package org.hibernate.ejb.metamodel;
import javax.persistence.metamodel.MappedSuperclassType;

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

	@Override
	protected boolean requiresSupertypeForNonDeclaredIdentifier() {
		return false;
	}
}
