/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.metamodel;

import java.io.Serializable;
import javax.persistence.metamodel.EntityType;

/**
 * Defines the Hibernate implementation of the JPA {@link EntityType} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class EntityTypeImpl<X> 
		extends AbstractIdentifiableType<X>
		implements EntityType<X>, Serializable {
	private final String className;

	public EntityTypeImpl(
			Class<X> javaType,
			AbstractIdentifiableType<? super X> superType, 
			String className,
			boolean hasIdentifierProperty,
			boolean isVersioned) {
		super( javaType, superType, hasIdentifierProperty, isVersioned );
		this.className = className;
	}

	public String getName() {
		return className;
	}

	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	public Class<X> getBindableJavaType() {
		return getJavaType();
	}

	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}
}
