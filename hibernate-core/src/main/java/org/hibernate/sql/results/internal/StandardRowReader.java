/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.RowReaderMemento;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class StandardRowReader<T> implements RowReader<T> {
	private final List<DomainResultAssembler> resultAssemblers;
	private final List<Initializer> initializers;
	private final RowTransformer<T> rowTransformer;

	private final int assemblerCount;
	private final Callback callback;


	@SuppressWarnings("WeakerAccess")
	public StandardRowReader(
			List<DomainResultAssembler> resultAssemblers,
			List<Initializer> initializers,
			RowTransformer<T> rowTransformer,
			Callback callback) {
		this.resultAssemblers = resultAssemblers;
		this.initializers = initializers;
		this.rowTransformer = rowTransformer;

		this.assemblerCount = resultAssemblers.size();
		this.callback = callback;

		logDebugInfo();
	}

	protected void logDebugInfo() {
		// we'd really need some form of description for the assemblers and initializers for this
		// to be useful.
		//
		// todo (6.0) : consider whether this ^^ is worth it

//		if ( ! ResultsLogger.DEBUG_ENABLED ) {
//			return;
//		}

	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getResultJavaType() {
		if ( resultAssemblers.size() == 1 ) {
			return (Class<T>) resultAssemblers.get( 0 ).getAssembledJavaTypeDescriptor().getJavaType();
		}

		return (Class<T>) Object[].class;
	}

	@Override
	public List<Initializer> getInitializers() {
		return initializers;
	}

	@Override
	public T readRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		LoadingLogger.LOGGER.trace( "StandardRowReader#readRow" );

		coordinateInitializers( rowProcessingState, options );

		final Object[] resultRow = new Object[ assemblerCount ];

		for ( int i = 0; i < assemblerCount; i++ ) {
			final DomainResultAssembler assembler = resultAssemblers.get( i );
			LoadingLogger.LOGGER.debugf( "Calling top-level assembler (%i / %i) : %s", i, assemblerCount, assembler );
			resultRow[i] = assembler.assemble( rowProcessingState, options );
		}

		afterRow( rowProcessingState, options );

		return rowTransformer.transformRow( resultRow );
	}

	private void afterRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		LoadingLogger.LOGGER.trace( "StandardRowReader#afterRow" );

		// todo (6.0) : add AfterLoadActions handling here via Callback
		//
		// maybe :
		// 		initializer.finishUpRow( rowProcessingState, callback );

		for ( Initializer initializer : initializers ) {
			initializer.finishUpRow( rowProcessingState );
		}
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void coordinateInitializers(
			RowProcessingState rowProcessingState,
			JdbcValuesSourceProcessingOptions options) {

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// todo (6.0) : we may want to split handling of initializers into specific sub-type handling
		//		- meaning we'd have something like:

//		for ( EntityInitializer initializer : entityInitializers ) {
//			initializer.resolveKey( rowProcessingState );
//		}
//
//		for ( EntityInitializer initializer : collectionInitializers ) {
//			initializer.resolveKey( rowProcessingState );
//		}
//
//		for ( Initializer initializer : entityInitializers ) {
//			initializer.resolveInstance( rowProcessingState );
//		}
//
//		for ( EntityInitializer initializer : collectionInitializers ) {
//			initializer.resolveInstance( rowProcessingState );
//		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// old

		final int numberOfInitializers = initializers.size();

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			final Initializer initializer = initializers.get( i );
			if ( ! ( initializer instanceof CollectionInitializer ) ) {
				initializer.resolveKey( rowProcessingState );
			}
		}

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			final Initializer initializer = initializers.get( i );
			if ( initializer instanceof CollectionInitializer ) {
				initializer.resolveKey( rowProcessingState );
			}
		}

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			initializers.get( i ).resolveInstance( rowProcessingState );
		}

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			initializers.get( i ).initializeInstance( rowProcessingState );
		}
	}

	@Override
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public void finishUp(JdbcValuesSourceProcessingState processingState) {
		for ( int i = 0; i < initializers.size(); i++ ) {
			initializers.get( i ).endLoading( processingState.getExecutionContext() );
		}

		// todo : use Callback to execute AfterLoadActions
		// todo : another option is to use Callback to execute the AfterLoadActions after each row
	}

	@Override
	public RowReaderMemento toMemento(SessionFactoryImplementor factory) {
		return new RowReaderMemento() {
			@Override
			public Class<?>[] getResultClasses() {
				return new Class[0];
			}

			@Override
			public String[] getResultMappingNames() {
				return new String[0];
			}
		};
	}
}
