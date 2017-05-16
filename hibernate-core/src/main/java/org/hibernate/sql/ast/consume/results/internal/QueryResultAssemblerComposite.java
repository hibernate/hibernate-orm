/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.results.internal;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.ast.consume.results.spi.QueryResultAssembler;
import org.hibernate.sql.ast.consume.results.spi.RowProcessingState;
import org.hibernate.sql.ast.produce.result.internal.QueryResultCompositeImpl;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class QueryResultAssemblerComposite implements QueryResultAssembler {
	private final QueryResultCompositeImpl returnComposite;
	private final List<SqlSelection> sqlSelections;
	private final EmbeddedPersister embeddedPersister;

	public QueryResultAssemblerComposite(
			QueryResultCompositeImpl returnComposite,
			List<SqlSelection> sqlSelections,
			EmbeddedPersister embeddedPersister) {

		this.returnComposite = returnComposite;
		this.sqlSelections = sqlSelections;
		this.embeddedPersister = embeddedPersister;
	}

	@Override
	public Class getReturnedJavaType() {
		return returnComposite.getReturnedJavaType();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		// for now has to be a very basic CompositeType (aka one-level && attributes of BasicType)
		final Object[] values = new Object[ sqlSelections.size() ];
		for ( int i = 0; i < sqlSelections.size(); i++ ) {
			values[i] = rowProcessingState.getJdbcValues()[ sqlSelections.get( i ).getValuesArrayPosition() ];
		}

		throw new NotYetImplementedException(  );
//		try {
//			final Object result = embeddedPersister.getReturnedClass().newInstance();
//			embeddedPersister.setPropertyValues( result, values, EntityMode.POJO );
//			return result;
//		}
//		catch (Exception e) {
//			throw new RuntimeException( "Unable to instantiate composite : " +  embeddedPersister.getReturnedClass().getName(), e );
//		}
	}
}
