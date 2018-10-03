/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;

import org.hibernate.metamodel.model.domain.spi.BasicTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeImplementor;

/**
 * @author Emmanuel Bernard
 */
public class BasicTypeImpl<J> implements BasicTypeImplementor<J>, SimpleTypeImplementor<J>, Serializable {
	private final Class<J> clazz;
	private PersistenceType persistenceType;

	public PersistenceType getPersistenceType() {
		return persistenceType;
	}

	public Class<J> getJavaType() {
		return clazz;
	}

	public BasicTypeImpl(Class<J> clazz, PersistenceType persistenceType) {
		this.clazz = clazz;
		this.persistenceType = persistenceType;
	}
}
