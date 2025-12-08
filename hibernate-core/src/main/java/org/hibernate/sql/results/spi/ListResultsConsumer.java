/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.spi;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EntityJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ResultsConsumer for creating a List of results
 *
 * @author Steve Ebersole
 */
public class ListResultsConsumer<R> implements ResultsConsumer<List<R>, R> {

	/**
	 * Let's be reasonable: a row estimate greater than 8k rows is probably either a misestimate or a bug,
	 * so let's set {@code 2^13} which is a bit above 8k as maximum collection size.
	 */
	private static final int INITIAL_COLLECTION_SIZE_LIMIT = 1 << 13;

	private static final ListResultsConsumer<?> NEVER_DE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.NEVER );
	private static final ListResultsConsumer<?> ALLOW_DE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.ALLOW );
	private static final ListResultsConsumer<?> IGNORE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.NONE );
	private static final ListResultsConsumer<?> DE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.FILTER );
	private static final ListResultsConsumer<?> ERROR_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.ASSERT );

	@SuppressWarnings("unchecked")
	public static <R> ListResultsConsumer<R> instance(UniqueSemantic uniqueSemantic) {
		return (ListResultsConsumer<R>) switch ( uniqueSemantic ) {
			case ASSERT -> ERROR_DUP_CONSUMER;
			case FILTER -> DE_DUP_CONSUMER;
			case NEVER -> NEVER_DE_DUP_CONSUMER;
			case ALLOW -> ALLOW_DE_DUP_CONSUMER;
			default -> IGNORE_DUP_CONSUMER;
		};
	}

	/**
	 * Ways this consumer can handle in-memory row de-duplication
	 */
	public enum UniqueSemantic {
		/**
		 * Apply no in-memory de-duplication
		 */
		NONE,

		/**
		 * Apply in-memory de-duplication, removing rows already part of the results
		 */
		FILTER,

		/**
		 * Apply in-memory duplication checks, throwing a HibernateException when duplicates are found
		 */
		ASSERT,

		/**
		 * Never apply unique handling.  E.g. for NativeQuery.  Whereas {@link #NONE} can be adjusted,
		 * NEVER will never apply unique handling
		 */
		NEVER,

		/**
		 * De-duplication is allowed if the query and result type allow
		 */
		ALLOW
	}

	private final UniqueSemantic uniqueSemantic;

	public ListResultsConsumer(UniqueSemantic uniqueSemantic) {
		this.uniqueSemantic = uniqueSemantic;
	}

	private static class Results<R> {
		private final ArrayList<R> results;
		private final JavaType<R> resultJavaType;

		public Results(JavaType<R> resultJavaType, int initialSize) {
			this.resultJavaType = resultJavaType;
			this.results = initialSize > 0 ? new ArrayList<>( initialSize ) : new ArrayList<>();
		}

		public boolean addUnique(R result) {
			for ( int i = 0; i < results.size(); i++ ) {
				if ( resultJavaType.areEqual( results.get( i ), result ) ) {
					return false;
				}
			}
			results.add( result );
			return true;
		}

		public void add(R result) {
			results.add( result );
		}

		public List<R> getResults() {
			return results;
		}
	}

	private static class EntityResult<R> extends Results<R> {
		private static final Object DUMP_VALUE = new Object();

		private final IdentityHashMap<R, Object> added;

		public EntityResult(JavaType<R> resultJavaType, int initialSize) {
			super( resultJavaType, initialSize );
			added = initialSize > 0 ? new IdentityHashMap<>( initialSize ) : new IdentityHashMap<>();
		}

		public boolean addUnique(R result) {
			if ( added.put( result, DUMP_VALUE ) == null ) {
				super.add( result );
				return true;
			}
			return false;
		}
	}

	@Override
	public List<R> consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader) {
		rowReader.startLoading( rowProcessingState );

		RuntimeException ex = null;
		final var persistenceContext = session.getPersistenceContextInternal();
		persistenceContext.beforeLoad();
		persistenceContext.getLoadContexts().register( jdbcValuesSourceProcessingState );
		try {
			final var domainResultJavaType = resolveDomainResultJavaType(
					rowReader.getDomainResultResultJavaType(),
					rowReader.getResultJavaTypes(),
					session.getTypeConfiguration()
			);

			final boolean isEntityResultType = domainResultJavaType instanceof EntityJavaType;
			final int initialCollectionSize = Math.min( jdbcValues.getResultCountEstimate(), INITIAL_COLLECTION_SIZE_LIMIT );
			final var results = createResults( isEntityResultType, domainResultJavaType, initialCollectionSize );
			final int readRows = readRows( rowProcessingState, rowReader, isEntityResultType, results );
			rowReader.finishUp( rowProcessingState );
			jdbcValuesSourceProcessingState.finishUp( readRows > 1 );
			return transformList( rowProcessingState, results );
		}
		catch (RuntimeException e) {
			ex = e;
		}
		finally {
			try {
				jdbcValues.finishUp( session );
				persistenceContext.afterLoad();
				persistenceContext.getLoadContexts().deregister( jdbcValuesSourceProcessingState );
				persistenceContext.initializeNonLazyCollections();
			}
			catch (RuntimeException e) {
				if ( ex != null ) {
					ex.addSuppressed( e );
				}
				else {
					ex = e;
				}
			}
			finally {
				if ( ex != null ) {
					throw ex;
				}
			}
		}
		throw new IllegalStateException( "Should not reach this" );
	}

	private static <R> List<R> transformList(ExecutionContext executionContext, Results<R> results) {
		final ResultListTransformer<R> transformer = getResultListTransformer( executionContext );
		return transformer == null ? results.getResults() : transformer.transformList( results.getResults() );
	}

	@SuppressWarnings("unchecked")
	private static <R> ResultListTransformer<R> getResultListTransformer(ExecutionContext executionContext) {
		return (ResultListTransformer<R>) executionContext.getQueryOptions().getResultListTransformer();
	}

	private Results<R> createResults(
			boolean isEntityResultType,
			JavaType<R> domainResultJavaType,
			int initialCollectionSize) {
		if ( isEntityResultType
			&& ( uniqueSemantic == UniqueSemantic.ALLOW || uniqueSemantic == UniqueSemantic.FILTER ) ) {
			return new EntityResult<>( domainResultJavaType, initialCollectionSize );
		}
		else {
			return new Results<>( domainResultJavaType, initialCollectionSize );
		}
	}

	private int readRows(
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader,
			boolean isEntityResultType,
			Results<R> results) {
		if ( uniqueSemantic == UniqueSemantic.FILTER
				|| uniqueSemantic == UniqueSemantic.ASSERT && rowReader.hasCollectionInitializers()
				|| uniqueSemantic == UniqueSemantic.ALLOW && isEntityResultType ) {
			return readUnique( rowProcessingState, rowReader, results );
		}
		else if ( uniqueSemantic == UniqueSemantic.ASSERT ) {
			return readUniqueAssert( rowProcessingState, rowReader, results );
		}
		else {
			return read( rowProcessingState, rowReader, results );
		}
	}

	private static <R> int read(
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader,
			Results<R> results) {
		int readRows = 0;
		while ( rowProcessingState.next() ) {
			results.add( rowReader.readRow( rowProcessingState ) );
			rowProcessingState.finishRowProcessing( true );
			readRows++;
		}
		return readRows;
	}

	private static <R> int readUniqueAssert(
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader,
			Results<R> results) {
		int readRows = 0;
		while ( rowProcessingState.next() ) {
			if ( !results.addUnique( rowReader.readRow( rowProcessingState ) ) ) {
				throw new HibernateException(
						String.format(
								Locale.ROOT,
								"Duplicate row was found and `%s` was specified",
								UniqueSemantic.ASSERT
						)
				);
			}
			rowProcessingState.finishRowProcessing( true );
			readRows++;
		}
		return readRows;
	}

	private static <R> int readUnique(
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader,
			Results<R> results) {
		int readRows = 0;
		while ( rowProcessingState.next() ) {
			final boolean added = results.addUnique( rowReader.readRow( rowProcessingState ) );
			rowProcessingState.finishRowProcessing( added );
			readRows++;
		}
		return readRows;
	}

	@SuppressWarnings("unchecked") //TODO: fix the unchecked casts
	private JavaType<R> resolveDomainResultJavaType(
			Class<R> domainResultResultJavaType,
			List<@Nullable JavaType<?>> resultJavaTypes,
			TypeConfiguration typeConfiguration) {
		final var javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();

		if ( domainResultResultJavaType != null ) {
			final var resultJavaType = javaTypeRegistry.resolveDescriptor( domainResultResultJavaType );
			// Could be that the user requested a more general type than the actual type,
			// so resolve the most concrete type since this type is used to determine equality of objects
			if ( resultJavaTypes.size() == 1
					&& isMoreConcrete( resultJavaType, resultJavaTypes.get( 0 ) ) ) {
				return (JavaType<R>) resultJavaTypes.get( 0 );
			}
			return resultJavaType;
		}

		if ( resultJavaTypes.size() == 1 ) {
			final var firstJavaType = resultJavaTypes.get( 0 );
			return firstJavaType == null
					? (JavaType<R>) javaTypeRegistry.resolveDescriptor( Object.class )
					: (JavaType<R>) firstJavaType;
		}
		else {
			return (JavaType<R>) javaTypeRegistry.resolveDescriptor( Object[].class );
		}
	}

	private static boolean isMoreConcrete(JavaType<?> resultJavaType, @Nullable JavaType<?> javaType) {
		return javaType != null
			&& resultJavaType.getJavaTypeClass().isAssignableFrom( javaType.getJavaTypeClass() );
	}

	@Override
	public boolean canResultsBeCached() {
		return true;
	}

	@Override
	public String toString() {
		return "ListResultsConsumer(" + uniqueSemantic + ")";
	}
}
