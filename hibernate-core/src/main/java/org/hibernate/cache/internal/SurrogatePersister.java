/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Middleware LLC or third-party contributors as
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
 *
 */
package org.hibernate.cache.internal;

import org.hibernate.persister.Persister;
import org.hibernate.type.Type;


/**
 * SurrogatePersister. This class was created as Deprecated already:
 * the intention is to use it temporarily only to avoid breaking some APIs
 * while improving the implementation of org.hibernate.cache.spi.CacheKey.
 * 
 * @deprecated
 * @author Sanne Grinovero
 * @since 4.3
 */
@Deprecated
public final class SurrogatePersister implements Persister {

	private final Type identifierType;
	private final String role;

	/**
	 * Create a new SurrogatePersister.
	 * 
	 * @param identifierType
	 * @param role
	 */
	public SurrogatePersister(Type identifierType, String role) {
		this.identifierType = identifierType;
		this.role = role;
	}

	@Override
	public Type getIdentifierType() {
		return identifierType;
	}

	@Override
	public String getRole() {
		return role;
	}

}
