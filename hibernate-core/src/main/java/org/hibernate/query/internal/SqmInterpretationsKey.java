/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.query.spi.QueryInterpretations;
import org.hibernate.sqm.query.SqmStatement;

/**
 * @author Steve Ebersole
 */
public class SqmInterpretationsKey implements QueryInterpretations.Key {
	private final SqmStatement sqmStatement;
	private final Class resultType;

	// certain QueryOptions would effect cache hits:
	//		firstRow
	//		maxRow
	// 		LockOptions
	//		databaseHints (possibly)
	// 		others?

	public SqmInterpretationsKey(SqmStatement sqmStatement, Class resultType) {
		this.sqmStatement = sqmStatement;
		this.resultType = resultType;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final SqmInterpretationsKey that = (SqmInterpretationsKey) o;
		return sqmStatement.equals( that.sqmStatement )
				&& ( resultType != null ? resultType.equals( that.resultType ) : that.resultType == null );

	}

	@Override
	public int hashCode() {
		int result = sqmStatement.hashCode();
		result = 31 * result + ( resultType != null ? resultType.hashCode() : 0 );
		return result;
	}
}
