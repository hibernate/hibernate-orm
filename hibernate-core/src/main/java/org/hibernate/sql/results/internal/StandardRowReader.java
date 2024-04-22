/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.embeddable.internal.NonAggregatedIdentifierMappingResultInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectJavaType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class StandardRowReader<T> implements RowReader<T> {
	private final List<DomainResultAssembler<?>> resultAssemblers;
	private final InitializersList initializers;
	private final RowTransformer<T> rowTransformer;
	private final Class<T> domainResultJavaType;

	private final int assemblerCount;
	private JavaType<?> assemblersResultJavaType;

	private static final Logger LOGGER = LoadingLogger.LOGGER;

	public StandardRowReader(
			List<DomainResultAssembler<?>> resultAssemblers,
			InitializersList initializers,
			RowTransformer<T> rowTransformer,
			Class<T> domainResultJavaType) {
		this.resultAssemblers = resultAssemblers;
		this.initializers = initializers;
		this.rowTransformer = rowTransformer;
		this.assemblerCount = resultAssemblers.size();
		this.domainResultJavaType = domainResultJavaType;
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
			javaTypes.add( getAssembledJavaType( resultAssembler ) );
		}
		return javaTypes;
	}

	@Override
	@Deprecated
	public List<Initializer> getInitializers() {
		return initializers.asList();
	}

	@Override
	public InitializersList getInitializersList() {
		return initializers;
	}

	@Override
	public T readRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		LOGGER.trace( "StandardRowReader#readRow" );
		coordinateInitializers( rowProcessingState );

		if ( assemblersResultJavaType == null ) {
			assemblersResultJavaType = getResultAssemblersJavaType();
		}
		final Object[] resultRow = new Object[assemblerCount];
		final boolean debugEnabled = LOGGER.isDebugEnabled();

		for ( int i = 0; i < assemblerCount; i++ ) {
			final DomainResultAssembler assembler = resultAssemblers.get( i );
			if ( debugEnabled ) {
				LOGGER.debugf( "Calling top-level assembler (%s / %s) : %s", i, assemblerCount, assembler );
			}
			resultRow[i] = assembler.assemble( rowProcessingState, options );
		}

		afterRow( rowProcessingState );

		if ( assemblersResultJavaType == null ) {
			assemblersResultJavaType = getResultAssemblersJavaType();
		}
		return rowTransformer.transformRow( resultRow, assemblersResultJavaType );
	}

	private void afterRow(RowProcessingState rowProcessingState) {
		LOGGER.trace( "StandardRowReader#afterRow" );
		initializers.finishUpRow( rowProcessingState );
	}

	private void coordinateInitializers(RowProcessingState rowProcessingState) {
		initializers.resolveKeys( rowProcessingState );
		initializers.resolveInstances( rowProcessingState );
		initializers.initializeInstance( rowProcessingState );
	}

	@Override
	public void finishUp(JdbcValuesSourceProcessingState processingState) {
		initializers.endLoading( processingState.getExecutionContext() );
	}

	private JavaType<?> getResultAssemblersJavaType() {
		JavaType<?> result = null;
		for ( DomainResultAssembler resultAssembler : resultAssemblers ) {
			final JavaType assembledJavaType = getAssembledJavaType( resultAssembler );
			if ( result != ObjectJavaType.INSTANCE ) {
				if ( assembledJavaType == null ) {
					return ObjectJavaType.INSTANCE;
				}
				else {
					if ( result == null ) {
						result = assembledJavaType;
					}
					else if ( result != assembledJavaType ) {
						return ObjectJavaType.INSTANCE;
					}
				}
			}
			else {
				return result;
			}
		}
		return result;
	}

	private static JavaType getAssembledJavaType(DomainResultAssembler resultAssembler) {
		final Initializer initializer = resultAssembler.getInitializer();
		if ( initializer != null && initializer instanceof NonAggregatedIdentifierMappingResultInitializer ) {
			return ( (NonAggregatedIdentifierMappingResultInitializer) initializer ).getJavaType();
		}
		return resultAssembler.getAssembledJavaType();
	}

}
