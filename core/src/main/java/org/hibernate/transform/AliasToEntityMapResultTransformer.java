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

import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

/**
 * {@link ResultTransformer} implementation which builds a map for each "row", made up  of each aliased value
 * where the alias is the map key.
 * <p/>
 * Since this transformer is stateless, all instances would be considered equal.  So for optimization purposes
 * we limit it to a single, singleton {@link #INSTANCE instance} (this is not quite true yet, see deprecation notice
 * on {@link #AliasToEntityMapResultTransformer() constructor}).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class AliasToEntityMapResultTransformer extends BasicTransformerAdapter implements Serializable {

	public static final AliasToEntityMapResultTransformer INSTANCE = new AliasToEntityMapResultTransformer();

	/**
	 * Instantiate AliasToEntityMapResultTransformer.
	 * <p/>
	 * todo : make private, see deprecation...
	 *
	 * @deprecated Use the {@link #INSTANCE} reference instead of explicitly creating a new one (to be removed in 3.4).
	 */
	public AliasToEntityMapResultTransformer() {
	}

	/**
	 * {@inheritDoc}
	 */
	public Object transformTuple(Object[] tuple, String[] aliases) {
		Map result = new HashMap(tuple.length);
		for ( int i=0; i<tuple.length; i++ ) {
			String alias = aliases[i];
			if ( alias!=null ) {
				result.put( alias, tuple[i] );
			}
		}
		return result;
	}

	/**
	 * Serialization hook for ensuring singleton uniqueing.
	 *
	 * @return The singleton instance : {@link #INSTANCE}
	 */
	private Object readResolve() {
		return INSTANCE;
	}


	// all AliasToEntityMapResultTransformer are considered equal ~~~~~~~~~~~~~

	/**
	 * All AliasToEntityMapResultTransformer are considered equal
	 *
	 * @param other The other instance to check for equality
	 * @return True if (non-null) other is a instance of AliasToEntityMapResultTransformer.
	 */
	public boolean equals(Object other) {
		// todo : we can remove this once the deprecated ctor can be made private...
		return other != null && AliasToEntityMapResultTransformer.class.isInstance( other );
	}

	/**
	 * All AliasToEntityMapResultTransformer are considered equal
	 *
	 * @return We simply return the hashCode of the AliasToEntityMapResultTransformer class name string.
	 */
	public int hashCode() {
		// todo : we can remove this once the deprecated ctor can be made private...
		return getClass().getName().hashCode();
	}
}
