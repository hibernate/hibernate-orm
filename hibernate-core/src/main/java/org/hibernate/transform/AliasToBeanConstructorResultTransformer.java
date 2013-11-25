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
import java.lang.reflect.Constructor;
import java.util.List;

import org.hibernate.QueryException;

/**
 * Wraps the tuples in a constructor call.
 *
 * todo : why Alias* in the name???
 */
public class AliasToBeanConstructorResultTransformer implements ResultTransformer {

	private final Constructor constructor;

	/**
	 * Instantiates a AliasToBeanConstructorResultTransformer.
	 *
	 * @param constructor The contructor in which to wrap the tuples.
	 */
	public AliasToBeanConstructorResultTransformer(Constructor constructor) {
		this.constructor = constructor;
	}
	
	/**
	 * Wrap the incoming tuples in a call to our configured constructor.
	 */
	@Override
	public Object transformTuple(Object[] tuple, String[] aliases) {
		try {
			return constructor.newInstance( tuple );
		}
		catch ( Exception e ) {
			throw new QueryException( 
					"could not instantiate class [" + constructor.getDeclaringClass().getName() + "] from tuple",
					e
			);
		}
	}

	@Override
	public List transformList(List collection) {
		return collection;
	}

	/**
	 * Define our hashCode by our defined constructor's hasCode.
	 *
	 * @return Our defined ctor hashCode
	 */
	@Override
	public int hashCode() {
		return constructor.hashCode();
	}

	/**
	 * 2 AliasToBeanConstructorResultTransformer are considered equal if they have the same
	 * defined constructor.
	 *
	 * @param other The other instance to check for equality.
	 * @return True if both have the same defined constuctor; false otherwise.
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof AliasToBeanConstructorResultTransformer
				&& constructor.equals( ( ( AliasToBeanConstructorResultTransformer ) other ).constructor );
	}
}
