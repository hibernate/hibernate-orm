/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CompositeQueryResultAssembler implements QueryResultAssembler {
	private final CompositeQueryResultImpl returnComposite;
	private final List<SqlSelection> sqlSelections;
	private final EmbeddedTypeDescriptor embeddedPersister;

	public CompositeQueryResultAssembler(
			CompositeQueryResultImpl returnComposite,
			List<SqlSelection> sqlSelections,
			EmbeddedTypeDescriptor embeddedPersister) {

		this.returnComposite = returnComposite;
		this.sqlSelections = sqlSelections;
		this.embeddedPersister = embeddedPersister;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return returnComposite.getJavaTypeDescriptor();
	}

	@Override
	public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		// for now has to be a very basic CompositeType (aka one-level && attributes of BasicType)
		final Object[] values = new Object[ sqlSelections.size() ];
		for ( int i = 0; i < sqlSelections.size(); i++ ) {
			values[i] = rowProcessingState.getJdbcValue( sqlSelections.get( i ) );
		}

		throw new NotYetImplementedFor6Exception(  );
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
