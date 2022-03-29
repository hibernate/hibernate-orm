/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.ArrayList;
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

/**
 * ResultsConsumer for creating a List of results
 *
 * @author Steve Ebersole
 */
public class ListResultsConsumer<R> implements ResultsConsumer<List<R>, R> {
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
	private final ResultHandler<R> resultHandler;

	public ListResultsConsumer(UniqueSemantic uniqueSemantic) {
		this.uniqueSemantic = uniqueSemantic;

		if ( uniqueSemantic == UniqueSemantic.FILTER ) {
			resultHandler = ListResultsConsumer::deDuplicationHandling;
		}
		else if ( uniqueSemantic == UniqueSemantic.ASSERT ) {
			resultHandler = ListResultsConsumer::duplicationErrorHandling;
		}
		else {
			resultHandler = ListResultsConsumer::applyAll;
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
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final TypeConfiguration typeConfiguration = session.getTypeConfiguration();
		final QueryOptions queryOptions = rowProcessingState.getQueryOptions();

		RuntimeException ex = null;
		try {
			persistenceContext.getLoadContexts().register( jdbcValuesSourceProcessingState );

			final List<R> results = new ArrayList<>();

			final JavaType<R> domainResultJavaType = resolveDomainResultJavaType(
					rowReader.getDomainResultResultJavaType(),
					rowReader.getResultJavaTypes(),
					typeConfiguration
			);

			final ResultHandler<R> resultHandlerToUse;

			if ( uniqueSemantic == UniqueSemantic.ALLOW
					&& domainResultJavaType instanceof EntityJavaType ) {
				resultHandlerToUse = ListResultsConsumer::deDuplicationHandling;
			}
			else {
				resultHandlerToUse = this.resultHandler;
			}

			while ( rowProcessingState.next() ) {
				final R row = rowReader.readRow( rowProcessingState, processingOptions );
				resultHandlerToUse.handle( row, domainResultJavaType, results, rowProcessingState );
				rowProcessingState.finishRowProcessing();
			}

			try {
				jdbcValuesSourceProcessingState.finishUp();
			}
			finally {
				persistenceContext.getLoadContexts().deregister( jdbcValuesSourceProcessingState );
			}

			//noinspection unchecked
			final ResultListTransformer<R> resultListTransformer = (ResultListTransformer<R>) queryOptions.getResultListTransformer();
			if ( resultListTransformer != null ) {
				return resultListTransformer.transformList( results );
			}

			return results;
		}
		catch (RuntimeException e) {
			ex = e;
		}
		finally {
			try {
				rowReader.finishUp( jdbcValuesSourceProcessingState );
				jdbcValues.finishUp( session );
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
		throw new IllegalStateException( "Should not reach this!" );
	}

	/**
	 * Essentially a tri-consumer for applying the different duplication strategies.
	 *
	 * @see UniqueSemantic
	 */
	@FunctionalInterface
	private interface ResultHandler<R> {
		void handle(R result, JavaType<R> transformedJavaType, List<R> results, RowProcessingStateStandardImpl rowProcessingState);
	}

	public static <R> void deDuplicationHandling(
			R result,
			JavaType<R> transformedJavaType,
			List<R> results,
			RowProcessingStateStandardImpl rowProcessingState) {
		withDuplicationCheck(
				result,
				transformedJavaType,
				results,
				rowProcessingState,
				false
		);
	}

	private static <R> void withDuplicationCheck(
			R result,
			JavaType<R> transformedJavaType,
			List<R> results,
			RowProcessingStateStandardImpl rowProcessingState,
			boolean throwException) {
		boolean addResult = true;

		for ( int i = 0; i < results.size(); i++ ) {
			final R existingResult = results.get( i );
			if ( transformedJavaType.areEqual( result, existingResult ) ) {
				if ( throwException && ! rowProcessingState.hasCollectionInitializers ) {
					throw new HibernateException(
							String.format(
									Locale.ROOT,
									"Duplicate row was found and `%s` was specified",
									UniqueSemantic.ASSERT
							)
					);
				}

				addResult = false;
				break;
			}
		}

		if ( addResult ) {
			results.add( result );
		}
	}

	public static <R> void duplicationErrorHandling(
			R result,
			JavaType<R> transformedJavaType,
			List<R> results,
			RowProcessingStateStandardImpl rowProcessingState) {
		withDuplicationCheck(
				result,
				transformedJavaType,
				results,
				rowProcessingState,
				true
		);
	}

	public static <R> void applyAll(
			R result,
			JavaType<R> transformedJavaType,
			List<R> results,
			RowProcessingStateStandardImpl rowProcessingState) {
		results.add( result );
	}

	private JavaType<R> resolveDomainResultJavaType(
			Class<R> domainResultResultJavaType,
			List<JavaType<?>> resultJavaTypes,
			TypeConfiguration typeConfiguration) {
		final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();

		if ( domainResultResultJavaType != null ) {
			return javaTypeRegistry.resolveDescriptor( domainResultResultJavaType );
		}

		if ( resultJavaTypes.size() == 1 ) {
			//noinspection unchecked
			return (JavaType<R>) resultJavaTypes.get( 0 );
		}

		return javaTypeRegistry.resolveDescriptor( Object[].class );
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
