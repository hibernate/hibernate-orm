package org.hibernate.ejb.metamodel;

import java.io.Serializable;
import javax.persistence.metamodel.BasicType;
import javax.persistence.metamodel.Type;

/**
 * @author Emmanuel Bernard
 */
public class BasicTypeImpl<X> implements BasicType<X>, Serializable {
	private final Class<X> clazz;
	private PersistenceType persistenceType;

	public PersistenceType getPersistenceType() {
		return persistenceType;
	}

	public Class<X> getJavaType() {
		return clazz;
	}

	public BasicTypeImpl(Class<X> clazz, PersistenceType persistenceType) {
		this.clazz = clazz;
		this.persistenceType = persistenceType;
	}
}
