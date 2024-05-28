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
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;
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
	private final DomainResultAssembler<?>[] resultAssemblers;
	private final Initializer<InitializerData>[] resultInitializers;
	private final InitializerData[] resultInitializersData;
	private final Initializer<InitializerData>[] initializers;
	private final InitializerData[] initializersData;
	private final Initializer<InitializerData>[] sortedForResolveInstance;
	private final InitializerData[] sortedForResolveInstanceData;
	private final boolean hasCollectionInitializers;
	private final RowTransformer<T> rowTransformer;
	private final Class<T> domainResultJavaType;

	private static final Logger LOGGER = LoadingLogger.LOGGER;

	public StandardRowReader(
			JdbcValuesMappingResolution jdbcValuesMappingResolution,
			RowTransformer<T> rowTransformer,
			Class<T> domainResultJavaType) {
		this(
				jdbcValuesMappingResolution.getDomainResultAssemblers(),
				jdbcValuesMappingResolution.getResultInitializers(),
				jdbcValuesMappingResolution.getInitializers(),
				jdbcValuesMappingResolution.getSortedForResolveInstance(),
				jdbcValuesMappingResolution.hasCollectionInitializers(),
				rowTransformer,
				domainResultJavaType
		);
	}

	public StandardRowReader(
			DomainResultAssembler<?>[] resultAssemblers,
			Initializer<?>[] resultInitializers,
			Initializer<?>[] initializers,
			Initializer<?>[] sortedForResolveInitializers,
			boolean hasCollectionInitializers,
			RowTransformer<T> rowTransformer,
			Class<T> domainResultJavaType) {
		this.resultAssemblers = resultAssemblers;
		this.resultInitializers = (Initializer<InitializerData>[]) resultInitializers;
		this.resultInitializersData = new InitializerData[resultInitializers.length];
		this.initializers = (Initializer<InitializerData>[]) initializers;
		this.initializersData = new InitializerData[initializers.length];
		this.sortedForResolveInstance = (Initializer<InitializerData>[]) sortedForResolveInitializers;
		this.sortedForResolveInstanceData = new InitializerData[sortedForResolveInstance.length];
		this.hasCollectionInitializers = hasCollectionInitializers;
		this.rowTransformer = rowTransformer;
		this.domainResultJavaType = domainResultJavaType;
	}

	@Override
	public Class<T> getDomainResultResultJavaType() {
		return domainResultJavaType;
	}

	@Override
	public Class<?> getResultJavaType() {
		if ( resultAssemblers.length == 1 ) {
			return resultAssemblers[0].getAssembledJavaType().getJavaTypeClass();
		}

		return Object[].class;
	}

	@Override
	public List<JavaType<?>> getResultJavaTypes() {
		List<JavaType<?>> javaTypes = new ArrayList<>( resultAssemblers.length );
		for ( DomainResultAssembler resultAssembler : resultAssemblers ) {
			javaTypes.add( resultAssembler.getAssembledJavaType() );
		}
		return javaTypes;
	}

	@Override
	public int getInitializerCount() {
		return initializers.length;
	}

	@Override
	public @Nullable EntityKey resolveSingleResultEntityKey(RowProcessingState rowProcessingState) {
		final EntityInitializer<?> entityInitializer = resultInitializers.length == 0
				? null
				: resultInitializers[0].asEntityInitializer();
		if ( entityInitializer == null ) {
			return null;
		}
		final EntityKey entityKey = entityInitializer.resolveEntityKeyOnly( rowProcessingState );
		finishUpRow();
		return entityKey;
	}

	@Override
	public boolean hasCollectionInitializers() {
		return hasCollectionInitializers;
	}

	@Override
	public T readRow(RowProcessingState rowProcessingState) {
		coordinateInitializers( rowProcessingState );

		final Object[] resultRow = new Object[ resultAssemblers.length ];

		for ( int i = 0; i < resultAssemblers.length; i++ ) {
			final DomainResultAssembler assembler = resultAssemblers[i];
			resultRow[i] = assembler.assemble( rowProcessingState );
		}

		afterRow( rowProcessingState );

		return rowTransformer.transformRow( resultRow );
	}

	private void afterRow(RowProcessingState rowProcessingState) {
		finishUpRow();
	}

	private void finishUpRow() {
		for ( InitializerData data : initializersData ) {
			data.setState( Initializer.State.UNINITIALIZED );
		}
	}

	private void coordinateInitializers(RowProcessingState rowProcessingState) {
		for ( int i = 0; i < resultInitializers.length; i++ ) {
			resultInitializers[i].resolveKey( resultInitializersData[i] );
		}
		for ( int i = 0; i < sortedForResolveInstance.length; i++ ) {
			if ( sortedForResolveInstanceData[i].getState() == Initializer.State.KEY_RESOLVED ) {
				sortedForResolveInstance[i].resolveInstance( sortedForResolveInstanceData[i] );
			}
		}
		for ( int i = 0; i < initializers.length; i++ ) {
			if ( initializersData[i].getState() == Initializer.State.RESOLVED ) {
				initializers[i].initializeInstance( initializersData[i] );
			}
		}
	}

	@Override
	public void startLoading(RowProcessingState processingState) {
		for ( int i = 0; i < resultInitializers.length; i++ ) {
			final Initializer<?> initializer = resultInitializers[i];
			initializer.startLoading( processingState );
			resultInitializersData[i] = initializer.getData( processingState );
		}
		for ( int i = 0; i < sortedForResolveInstance.length; i++ ) {
			sortedForResolveInstanceData[i] = sortedForResolveInstance[i].getData( processingState );
		}
		for ( int i = 0; i < initializers.length; i++ ) {
			initializersData[i] = initializers[i].getData( processingState );
		}
	}

	@Override
	public void finishUp(RowProcessingState rowProcessingState) {
		for ( int i = 0; i < initializers.length; i++ ) {
			initializers[i].endLoading( initializersData[i] );
		}
	}

}
