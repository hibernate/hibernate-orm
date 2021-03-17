/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * @param constructor The constructor in which to wrap the tuples.
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
	 * @return True if both have the same defined constructor; false otherwise.
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof AliasToBeanConstructorResultTransformer
				&& constructor.equals( ( ( AliasToBeanConstructorResultTransformer ) other ).constructor );
	}
}
