/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * A function that returns a {@link org.hibernate.type.StandardBasicTypes#TIMESTAMP}
 * with static fractional seconds precision (fsp).
 *
 * @author Gail Badner
 */
public class StaticPrecisionFspTimestampFunction extends NoArgSQLFunction {
	private final String renderedString;

	/**
	 * Constructs a {@link org.hibernate.type.StandardBasicTypes#TIMESTAMP} function using the
	 * default fractional second precision as defined by the database.
	 *
	 * @param name The function name
	 * @param hasParenthesesIfNoArguments Does the function call need parenthesis if there are no arguments?
	 */
	public StaticPrecisionFspTimestampFunction(String name, boolean hasParenthesesIfNoArguments) {
		super( name, StandardBasicTypes.TIMESTAMP, hasParenthesesIfNoArguments );
		renderedString = null;
	}

	/**
	 * Constructs a {@link org.hibernate.type.StandardBasicTypes#TIMESTAMP} function using
	 * the specified fractional seconds precision.
	 * @param name The function name
	 * @param fsp The explicit fractional seconds precision to render
	 *
	 * @throws java.lang.IllegalArgumentException if {@code fsp} < 0.
	 */
	public StaticPrecisionFspTimestampFunction(String name, int fsp) {
		super( name, StandardBasicTypes.TIMESTAMP);
		if ( fsp < 0 ) {
			throw new IllegalArgumentException( "fsp must be >= 0" );
		}
		renderedString = name + '(' + fsp + ')';
	}

	@Override
	public String render(Type argumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		if ( args.size() > 0 ) {
			throw new QueryException( "function takes no arguments: " + getName() );
		}
		return renderedString == null ?
				super.render( argumentType, args, factory ) :
				renderedString;
	}
}
