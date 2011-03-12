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
 *
 */
package org.hibernate.transform;

final public class Transformers {

	private Transformers() {}
	
	/**
	 * Each row of results is a <tt>Map</tt> from alias to values/entities
	 */
	public static final AliasToEntityMapResultTransformer ALIAS_TO_ENTITY_MAP =
			AliasToEntityMapResultTransformer.INSTANCE;

	/**
	 * Each row of results is a <tt>List</tt> 
	 */
	public static final ToListResultTransformer TO_LIST = ToListResultTransformer.INSTANCE;
	
	/**
	 * Creates a resulttransformer that will inject aliased values into 
	 * instances of Class via property methods or fields.
	 */
	public static ResultTransformer aliasToBean(Class target) {
		return new AliasToBeanResultTransformer(target);
	}
	
}
