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
package org.hibernate.dialect;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.util.Map;
import java.util.HashMap;

/**
 * Defines how we need to reference columns in the group-by, having, and order-by
 * clauses.
 *
 * @author Steve Ebersole
 */
public class ResultColumnReferenceStrategy implements Serializable {

	private static final Map INSTANCES = new HashMap();

	/**
	 * This strategy says to reference the result columns by the qualified column name
	 * found in the result source.  This strategy is not strictly allowed by ANSI SQL
	 * but is Hibernate's legacy behavior and is also the fastest of the strategies; thus
	 * it should be used if supported by the underlying database.
	 */
	public static final ResultColumnReferenceStrategy SOURCE = new ResultColumnReferenceStrategy( "source");

	/**
	 * For databases which do not support {@link #SOURCE}, ANSI SQL defines two allowable
	 * approaches.  One is to reference the result column by the alias it is given in the
	 * result source (if it is given an alias).  This strategy says to use this approach.
	 * <p/>
	 * The other QNSI SQL compliant approach is {@link #ORDINAL}.
	 */
	public static final ResultColumnReferenceStrategy ALIAS = new ResultColumnReferenceStrategy( "alias" );

	/**
	 * For databases which do not support {@link #SOURCE}, ANSI SQL defines two allowable
	 * approaches.  One is to reference the result column by the ordinal position at which
	 * it appears in the result source.  This strategy says to use this approach.
	 * <p/>
	 * The other QNSI SQL compliant approach is {@link #ALIAS}.
	 */
	public static final ResultColumnReferenceStrategy ORDINAL = new ResultColumnReferenceStrategy( "ordinal" );

	static {
		ResultColumnReferenceStrategy.INSTANCES.put( ResultColumnReferenceStrategy.SOURCE.name, ResultColumnReferenceStrategy.SOURCE );
		ResultColumnReferenceStrategy.INSTANCES.put( ResultColumnReferenceStrategy.ALIAS.name, ResultColumnReferenceStrategy.ALIAS );
		ResultColumnReferenceStrategy.INSTANCES.put( ResultColumnReferenceStrategy.ORDINAL.name, ResultColumnReferenceStrategy.ORDINAL );
	}

	private final String name;

	public ResultColumnReferenceStrategy(String name) {
		this.name = name;
	}

	public String toString() {
		return name;
	}

	private Object readResolve() throws ObjectStreamException {
		return parse( name );
	}

	public static ResultColumnReferenceStrategy parse(String name) {
		return ( ResultColumnReferenceStrategy ) ResultColumnReferenceStrategy.INSTANCES.get( name );
	}
}
