/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.process.internal;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.sql.convert.spi.Callback;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.exec.results.process.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.RowReader;
import org.hibernate.sql.exec.results.process.spi.EntityReferenceInitializer;
import org.hibernate.sql.exec.results.process.spi.Initializer;
import org.hibernate.sql.exec.results.process.spi.ReturnAssembler;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class RowReaderStandardImpl<T> implements RowReader<T> {
	private final List<ReturnAssembler> returnAssemblers;
	private final List<Initializer> initializers;
	private final RowTransformer<T> rowTransformer;

	private final int returnsCount;
	private final Callback callback;

	public RowReaderStandardImpl(
			List<ReturnAssembler> returnAssemblers,
			List<Initializer> initializers,
			RowTransformer<T> rowTransformer,
			Callback callback) {
		this.returnAssemblers = returnAssemblers;
		this.initializers = initializers;
		this.rowTransformer = rowTransformer;

		this.returnsCount = returnAssemblers.size();
		this.callback = callback;
	}

	@Override
	public T readRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) throws SQLException {
		// NOTE : atm we only support reading scalar values...
		// todo : support other stuff ^^

		coordinateInitializers( rowProcessingState, options );

		// finally assemble the results

		final Object[] result = new Object[returnsCount];
		for ( int i = 0; i < returnsCount; i++ ) {
			result[i] = returnAssemblers.get( i ).assemble( rowProcessingState, options );
		}

		// todo : add AfterLoadActions handling here via Callback

		return rowTransformer.transformRow( result );
	}

	private void coordinateInitializers(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {
		// todo : figure out CompositeReferenceInitializer handling
		// todo : figure out CollectionReferenceInitializer handling

		for ( Initializer initializer : initializers ) {
			if ( initializer instanceof EntityReferenceInitializer ) {
				( (EntityReferenceInitializer) initializer ).hydrateIdentifier( rowProcessingState );
			}
		}

		for ( Initializer initializer : initializers ) {
			if ( initializer instanceof EntityReferenceInitializer ) {
				( (EntityReferenceInitializer) initializer ).resolveEntityKey( rowProcessingState );
			}
		}

		for ( Initializer initializer : initializers ) {
			if ( initializer instanceof EntityReferenceInitializer ) {
				( (EntityReferenceInitializer) initializer ).hydrateEntityState( rowProcessingState );
			}
		}

		for ( Initializer initializer : initializers ) {
			initializer.finishUpRow( rowProcessingState );
		}

	}

	@Override
	public void finishUp(JdbcValuesSourceProcessingState context) {
		// todo : use Callback to execute AfterLoadActions
		// todo : another option is to use Callback to execute the AfterLoadActions after each row
	}
}
