/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.EntityJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ResultsConsumer for creating a List of results
 *
 * @author Steve Ebersole
 */
public class ListResultsConsumer<R> implements ResultsConsumer<List<R>, R> {

	/**
	 * Let's be reasonable, a row estimate greater than 1M rows is probably either a mis-estimation or bug,
	 * so let's set 2^20 which is a bit above 1M as maximum collection size.
	 */
	private static final int INITIAL_COLLECTION_SIZE_LIMIT = 1 << 20;

	private static final ListResultsConsumer<?> NEVER_DE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.NEVER );
	private static final ListResultsConsumer<?> ALLOW_DE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.ALLOW );
	private static final ListResultsConsumer<?> IGNORE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.NONE );
	private static final ListResultsConsumer<?> DE_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.FILTER );
	private static final ListResultsConsumer<?> ERROR_DUP_CONSUMER = new ListResultsConsumer<>( UniqueSemantic.ASSERT );

	@SuppressWarnings("unchecked")
	public static <R> ListResultsConsumer<R> instance(UniqueSemantic uniqueSemantic) {
		switch ( uniqueSemantic ) {
			case ASSERT: {
				return (ListResultsConsumer<R>) ERROR_DUP_CONSUMER;
			}
			case FILTER: {
				return (ListResultsConsumer<R>) DE_DUP_CONSUMER;
			}
			case NEVER: {
				return (ListResultsConsumer<R>) NEVER_DE_DUP_CONSUMER;
			}
			case ALLOW: {
				return (ListResultsConsumer<R>) ALLOW_DE_DUP_CONSUMER;
			}
			default: {
				return (ListResultsConsumer<R>) IGNORE_DUP_CONSUMER;
			}
		}
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
		private final List<R> results;
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
			JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader) {
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final TypeConfiguration typeConfiguration = session.getTypeConfiguration();
		final QueryOptions queryOptions = rowProcessingState.getQueryOptions();

		rowReader.startLoading( rowProcessingState );

		RuntimeException ex = null;
		persistenceContext.beforeLoad();
		persistenceContext.getLoadContexts().register( jdbcValuesSourceProcessingState );
		try {
			final JavaType<R> domainResultJavaType = resolveDomainResultJavaType(
					rowReader.getDomainResultResultJavaType(),
					rowReader.getResultJavaTypes(),
					typeConfiguration
			);

			final boolean isEntityResultType = domainResultJavaType instanceof EntityJavaType;
			final int initialCollectionSize = Math.min( jdbcValues.getResultCountEstimate(), INITIAL_COLLECTION_SIZE_LIMIT );

			final Results<R> results;
			if ( isEntityResultType
					&& ( uniqueSemantic == UniqueSemantic.ALLOW
						|| uniqueSemantic == UniqueSemantic.FILTER ) ) {
				results = new EntityResult<>( domainResultJavaType, initialCollectionSize );
			}
			else {
				results = new Results<>( domainResultJavaType, initialCollectionSize );
			}

			int readRows = 0;
			if ( uniqueSemantic == UniqueSemantic.FILTER
					|| uniqueSemantic == UniqueSemantic.ASSERT && rowReader.hasCollectionInitializers()
					|| uniqueSemantic == UniqueSemantic.ALLOW && isEntityResultType ) {
				while ( rowProcessingState.next() ) {
					final boolean added = results.addUnique( rowReader.readRow( rowProcessingState ) );
					rowProcessingState.finishRowProcessing( added );
					readRows++;
				}
			}
			else if ( uniqueSemantic == UniqueSemantic.ASSERT ) {
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
			}
			else {
				while ( rowProcessingState.next() ) {
					results.add( rowReader.readRow( rowProcessingState ) );
					rowProcessingState.finishRowProcessing( true );
					readRows++;
				}
			}

			rowReader.finishUp( rowProcessingState );
			jdbcValuesSourceProcessingState.finishUp( readRows > 1 );

			//noinspection unchecked
			final ResultListTransformer<R> resultListTransformer =
					(ResultListTransformer<R>) queryOptions.getResultListTransformer();
			if ( resultListTransformer != null ) {
				return resultListTransformer.transformList( results.getResults() );
			}

			return results.getResults();
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

	private JavaType<R> resolveDomainResultJavaType(
			Class<R> domainResultResultJavaType,
			List<@Nullable JavaType<?>> resultJavaTypes,
			TypeConfiguration typeConfiguration) {
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();

		if ( domainResultResultJavaType != null ) {
			final JavaType<R> resultJavaType = javaTypeRegistry.resolveDescriptor( domainResultResultJavaType );
			// Could be that the user requested a more general type than the actual type,
			// so resolve the most concrete type since this type is used to determine equality of objects
			if ( resultJavaTypes.size() == 1 && isMoreConcrete( resultJavaType, resultJavaTypes.get( 0 ) ) ) {
				//noinspection unchecked
				return (JavaType<R>) resultJavaTypes.get( 0 );
			}
			return resultJavaType;
		}

		if ( resultJavaTypes.size() == 1 ) {
			final JavaType<?> firstJavaType = resultJavaTypes.get( 0 );
			if ( firstJavaType == null ) {
				return javaTypeRegistry.resolveDescriptor( Object.class );
			}
			//noinspection unchecked
			return (JavaType<R>) firstJavaType;
		}

		return javaTypeRegistry.resolveDescriptor( Object[].class );
	}

	private static boolean isMoreConcrete(JavaType<?> resultJavaType, @Nullable JavaType<?> javaType) {
		return javaType != null && resultJavaType.getJavaTypeClass().isAssignableFrom( javaType.getJavaTypeClass() );
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
