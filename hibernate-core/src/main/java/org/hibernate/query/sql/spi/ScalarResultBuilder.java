/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.sql.ScalarResultRegistration;
import org.hibernate.query.sql.spi.ast.SqlSelectionImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.internal.select.QueryResultScalarImpl;
import org.hibernate.sql.ast.tree.spi.select.QueryResult;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.spi.ValueExtractor;

/**
 * @author Steve Ebersole
 */
public class ScalarResultBuilder
		implements ReturnableResultNodeImplementor, ScalarResultRegistration {
	private final String columnName;
	private final BasicValuedExpressableType type;

	public ScalarResultBuilder(String columnName) {
		this( columnName, null );
	}

	public ScalarResultBuilder(String columnName, BasicValuedExpressableType type) {
		this.columnName = columnName;
		this.type = type;
	}

	interface ExtractorResolver {
		ValueExtractor resolveExtractor();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ReturnableResultBuilder

	private ValueExtractor determineExtractor() {
		if ( type == null ) {
			return null;
		}

		// todo (6.0) - need to look at consolidating ValueExtractor and SqlSelectionReader which perform essentially the same function
		//			- ValueExtractor is *slightly* "lower level" operating on the ResultSet/CallableStatement
		//					and position/name
		//			- SqlSelectionReader also operates on the ResultSet/CallableStatement, but based on SqlSelection
		//
		//		long-story-short, both read values from JDBC and return them.   Generally speaking
		//			SqlSelectionReader "wraps" a ValueExtractor and dispatches its calls to that
		//			wrapped ValueExtractor.  Again generally speaking, the SqlSelectionReader result
		//			is used to populate the "current row JDBC values" array (

		//		both simply return the read JDBC value.  Generally speaking
		//			- ValueExtractor operates on the JDBC-level, directly on the ResultSet/CallableStatement
		//			- SqlSelectionReader works on the JdbcValuesSourceProcessingState-level (access to the
		//				already "extracted" JDBC values)

		return type.getBasicType().getValueExtractor();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ScalarResultRegistration

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public Type getType() {
		return (Type) type;
	}
}
