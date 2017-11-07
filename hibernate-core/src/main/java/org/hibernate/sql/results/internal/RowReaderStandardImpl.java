/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.EntityInitializer;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class RowReaderStandardImpl<T> implements RowReader<T> {
	private final List<QueryResultAssembler> resultAssemblers;
	private final List<Initializer> initializers;
	private final RowTransformer<T> rowTransformer;

	private final int assemblerCount;
	private final Callback callback;

	// todo (6.0) : partition the incoming `initializers` into separate Lists based on type
	//		e.g. `List<EntityInitializer>` versus `List<CollectionInitializer>` versus `List<ComponentInitializer>`

	public RowReaderStandardImpl(
			List<QueryResultAssembler> resultAssemblers,
			List<Initializer> initializers,
			RowTransformer<T> rowTransformer,
			Callback callback) {
		this.resultAssemblers = resultAssemblers;
		this.initializers = initializers;
		this.rowTransformer = rowTransformer;

		this.assemblerCount = resultAssemblers.size();
		this.callback = callback;
	}

	@Override
	public int getNumberOfResults() {
		return rowTransformer.determineNumberOfResultElements( assemblerCount );
	}

	@Override
	public T readRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		coordinateInitializers( rowProcessingState, options );

		// finally assemble the results

		final Object[] result = new Object[assemblerCount];
		for ( int i = 0; i < assemblerCount; i++ ) {
			result[i] = resultAssemblers.get( i ).assemble( rowProcessingState, options );
		}

		afterRow( rowProcessingState, options );

		return rowTransformer.transformRow( result );
	}

	private void afterRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		// todo : add AfterLoadActions handling here via Callback

		for ( Initializer initializer : initializers ) {
			initializer.finishUpRow( rowProcessingState );
		}
	}

	private void coordinateInitializers(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		// todo : figure out CompositeReferenceInitializer handling
		// todo : figure out CollectionReferenceInitializer handling

		for ( Initializer initializer : initializers ) {
			if ( initializer instanceof EntityInitializer ) {
				( (EntityInitializer) initializer ).hydrateIdentifier( rowProcessingState );
			}
		}

		for ( Initializer initializer : initializers ) {
			if ( initializer instanceof EntityInitializer ) {
				( (EntityInitializer) initializer ).resolveEntityKey( rowProcessingState );
			}
		}

		for ( Initializer initializer : initializers ) {
			if ( initializer instanceof EntityInitializer ) {
				( (EntityInitializer) initializer ).hydrateEntityState( rowProcessingState );
			}
		}

		// todo (6.0) : properly account for
		for ( Initializer initializer : initializers ) {
			if ( initializer instanceof EntityInitializer ) {
				( (EntityInitializer) initializer ).resolveEntityState( rowProcessingState );
			}
		}

	}

	@Override
	public void finishUp(JdbcValuesSourceProcessingState context) {
		// todo : use Callback to execute AfterLoadActions
		// todo : another option is to use Callback to execute the AfterLoadActions after each row
	}
}
