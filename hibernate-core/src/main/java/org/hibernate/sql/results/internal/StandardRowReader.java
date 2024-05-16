/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.descriptor.java.JavaType;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class StandardRowReader<T> implements RowReader<T> {
	private final List<DomainResultAssembler<?>> resultAssemblers;
	private final Initializer[] resultInitializers;
	private final InitializersList initializersList;
	private final boolean hasCollectionInitializers;
	private final RowTransformer<T> rowTransformer;
	private final Class<T> domainResultJavaType;

	private final int assemblerCount;

	private static final Logger LOGGER = LoadingLogger.LOGGER;

	public StandardRowReader(
			List<DomainResultAssembler<?>> resultAssemblers,
			boolean hasCollectionInitializers,
			InitializersList initializersList,
			RowTransformer<T> rowTransformer,
			Class<T> domainResultJavaType) {
		this.resultAssemblers = resultAssemblers;
		this.resultInitializers = getResultInitializers( resultAssemblers );
		this.initializersList = initializersList;
		this.hasCollectionInitializers = hasCollectionInitializers;
		this.rowTransformer = rowTransformer;
		this.assemblerCount = resultAssemblers.size();
		this.domainResultJavaType = domainResultJavaType;
	}

	private static Initializer[] getResultInitializers(List<DomainResultAssembler<?>> resultAssemblers) {
		final ArrayList<Initializer> initializers = new ArrayList<>( resultAssemblers.size() );
		for ( DomainResultAssembler<?> resultAssembler : resultAssemblers ) {
			resultAssembler.forEachResultAssembler( (initializer, list) -> list.add( initializer ), initializers );
		}
		return initializers.toArray(Initializer.EMPTY_ARRAY);
	}

	@Override
	public Class<T> getDomainResultResultJavaType() {
		return domainResultJavaType;
	}

	@Override
	public Class<?> getResultJavaType() {
		if ( resultAssemblers.size() == 1 ) {
			return resultAssemblers.get( 0 ).getAssembledJavaType().getJavaTypeClass();
		}

		return Object[].class;
	}

	@Override
	public List<JavaType<?>> getResultJavaTypes() {
		List<JavaType<?>> javaTypes = new ArrayList<>( resultAssemblers.size() );
		for ( DomainResultAssembler resultAssembler : resultAssemblers ) {
			javaTypes.add( resultAssembler.getAssembledJavaType() );
		}
		return javaTypes;
	}

	@Override
	@Deprecated
	public List<Initializer> getInitializers() {
		return initializersList.asList();
	}

	@Override
	public InitializersList getInitializersList() {
		return initializersList;
	}

	@Override
	public @Nullable EntityKey resolveSingleResultEntityKey(RowProcessingState rowProcessingState) {
		final EntityInitializer entityInitializer = resultInitializers.length == 0
				? null
				: resultInitializers[0].asEntityInitializer();
		if ( entityInitializer == null ) {
			return null;
		}
		final EntityKey entityKey = entityInitializer.resolveEntityKeyOnly( rowProcessingState );
		initializersList.finishUpRow();
		return entityKey;
	}

	@Override
	public boolean hasCollectionInitializers() {
		return hasCollectionInitializers;
	}

	@Override
	public T readRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		LOGGER.trace( "StandardRowReader#readRow" );
		coordinateInitializers();

		final Object[] resultRow = new Object[ assemblerCount ];
		final boolean debugEnabled = LOGGER.isDebugEnabled();

		for ( int i = 0; i < assemblerCount; i++ ) {
			final DomainResultAssembler assembler = resultAssemblers.get( i );
			if ( debugEnabled ) {
				LOGGER.debugf( "Calling top-level assembler (%s / %s) : %s", i, assemblerCount, assembler );
			}
			resultRow[i] = assembler.assemble( rowProcessingState, options );
		}

		afterRow();

		return rowTransformer.transformRow( resultRow );
	}

	private void afterRow() {
		LOGGER.trace( "StandardRowReader#afterRow" );
		initializersList.finishUpRow();
	}

	private void coordinateInitializers() {
		for ( Initializer initializer : resultInitializers ) {
			initializer.resolveKey();
		}
		initializersList.resolveInstances();
		initializersList.initializeInstance();
	}

	@Override
	public void startLoading(RowProcessingState processingState) {
		for ( Initializer initializer : resultInitializers ) {
			initializer.startLoading( processingState );
		}
	}

	@Override
	public void finishUp(JdbcValuesSourceProcessingState processingState) {
		initializersList.endLoading( processingState.getExecutionContext() );
	}

}
