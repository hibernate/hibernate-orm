/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.metamodel.binding;

import org.hibernate.MappingException;

/**
 * The inheritance type for a given entity.
 * <p>
 * Note, we are not using the JPA enum, because we need the ability to extend the types if we need to.
 *
 * @author Hardy Ferentschik
 */
public enum InheritanceType {
	JOINED,
	SINGLE_TABLE,
	TABLE_PER_CLASS,
	NO_INHERITANCE;

	/**
	 * @param jpaType The JPA inheritance type
	 *
	 * @return The inheritance type of this class.
	 */
	public static InheritanceType get(javax.persistence.InheritanceType jpaType) {
		switch ( jpaType ) {
			case SINGLE_TABLE: {
				return InheritanceType.SINGLE_TABLE;
			}
			case JOINED: {
				return InheritanceType.JOINED;
			}
			case TABLE_PER_CLASS: {
				return InheritanceType.TABLE_PER_CLASS;
			}
			default: {
				throw new MappingException( "Unknown jpa inheritance type:" + jpaType.name() );
			}
		}
	}
}

