/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class StandardAnsiSqlAggregationFunctions {
	/**
	 * Definition of a standard ANSI SQL compliant <tt>COUNT</tt> function
	 */
	public static class CountFunction extends StandardSQLFunction {
		public static final CountFunction INSTANCE = new CountFunction();

		public CountFunction() {
			super( "count", Hibernate.LONG );
		}

		@Override
		public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) {
			if ( arguments.size() > 1 ) {
				if ( "distinct".equalsIgnoreCase( arguments.get( 0 ).toString() ) ) {
					return renderCountDistinct( arguments );
				}
			}
			return super.render( firstArgumentType, arguments, factory );
		}

		private String renderCountDistinct(List arguments) {
			StringBuffer buffer = new StringBuffer();
			buffer.append( "count(distinct " );
			String sep = "";
			Iterator itr = arguments.iterator();
			itr.next(); // intentionally skip first
			while ( itr.hasNext() ) {
				buffer.append( sep )
						.append( itr.next() );
				sep = ", ";
			}
			return buffer.append( ")" ).toString();
		}
	}


	/**
	 * Definition of a standard ANSI SQL compliant <tt>AVG</tt> function
	 */
	public static class AvgFunction extends StandardSQLFunction {
		public static final AvgFunction INSTANCE = new AvgFunction();

		public AvgFunction() {
			super( "avg", Hibernate.DOUBLE );
		}

		@Override
		public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor factory) throws QueryException {
			int jdbcTypeCode = determineJdbcTypeCode( firstArgumentType, factory );
			return render( jdbcTypeCode, arguments.get(0).toString(), factory );
		}

		protected final int determineJdbcTypeCode(Type firstArgumentType, SessionFactoryImplementor factory) throws QueryException {
			try {
				final int[] jdbcTypeCodes = firstArgumentType.sqlTypes( factory );
				if ( jdbcTypeCodes.length != 1 ) {
					throw new QueryException( "multiple-column type in avg()" );
				}
				return jdbcTypeCodes[0];
			}
			catch ( MappingException me ) {
				throw new QueryException( me );
			}
		}

		protected String render(int firstArgumentJdbcType, String argument, SessionFactoryImplementor factory) {
			return "avg(" + renderArgument( argument, firstArgumentJdbcType ) + ")";
		}

		protected String renderArgument(String argument, int firstArgumentJdbcType) {
			return argument;
		}
	}


	public static class MaxFunction extends StandardSQLFunction {
		public static final MaxFunction INSTANCE = new MaxFunction();

		public MaxFunction() {
			super( "max" );
		}
	}

	public static class MinFunction extends StandardSQLFunction {
		public static final MinFunction INSTANCE = new MinFunction();

		public MinFunction() {
			super( "min" );
		}
	}


	public static class SumFunction extends StandardSQLFunction {
		public static final SumFunction INSTANCE = new SumFunction();

		public SumFunction() {
			super( "sum" );
		}

		protected final int determineJdbcTypeCode(Type type, Mapping mapping) throws QueryException {
			try {
				final int[] jdbcTypeCodes = type.sqlTypes( mapping );
				if ( jdbcTypeCodes.length != 1 ) {
					throw new QueryException( "multiple-column type in sum()" );
				}
				return jdbcTypeCodes[0];
			}
			catch ( MappingException me ) {
				throw new QueryException( me );
			}
		}

		public Type getReturnType(Type firstArgumentType, Mapping mapping) {
			final int jdbcType = determineJdbcTypeCode( firstArgumentType, mapping );

			// First allow the actual type to control the return value; the underlying sqltype could
			// actually be different
			if ( firstArgumentType == Hibernate.BIG_INTEGER ) {
				return Hibernate.BIG_INTEGER;
			}
			else if ( firstArgumentType == Hibernate.BIG_DECIMAL ) {
				return Hibernate.BIG_DECIMAL;
			}
			else if ( firstArgumentType == Hibernate.LONG
					|| firstArgumentType == Hibernate.SHORT
					|| firstArgumentType == Hibernate.INTEGER ) {
				return Hibernate.LONG;
			}
			else if ( firstArgumentType == Hibernate.FLOAT || firstArgumentType == Hibernate.DOUBLE)  {
				return Hibernate.DOUBLE;
			}

			// finally use the jdbcType if == on Hibernate types did not find a match.
			//
			//	IMPL NOTE : we do not match on Types.NUMERIC because it could be either, so we fall-through to the
			// 		first argument type
			if ( jdbcType == Types.FLOAT
					|| jdbcType == Types.DOUBLE
					|| jdbcType == Types.DECIMAL
					|| jdbcType == Types.REAL) {
				return Hibernate.DOUBLE;
			}
			else if ( jdbcType == Types.BIGINT
					|| jdbcType == Types.INTEGER
					|| jdbcType == Types.SMALLINT
					|| jdbcType == Types.TINYINT ) {
				return Hibernate.LONG;
			}

			// as a last resort, return the type of the first argument
			return firstArgumentType;
		}
	}

	public static void primeFunctionMap(Map<String, SQLFunction> functionMap) {
		functionMap.put( AvgFunction.INSTANCE.getName(), AvgFunction.INSTANCE );
		functionMap.put( CountFunction.INSTANCE.getName(), CountFunction.INSTANCE );
		functionMap.put( MaxFunction.INSTANCE.getName(), MaxFunction.INSTANCE );
		functionMap.put( MinFunction.INSTANCE.getName(), MinFunction.INSTANCE );
		functionMap.put( SumFunction.INSTANCE.getName(), SumFunction.INSTANCE );
	}
}
