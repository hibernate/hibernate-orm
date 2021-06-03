/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ListResultsConsumer<R> implements ResultsConsumer<List<R>, R> {
	/**
	 * Singleton access
	 */
	private static final ListResultsConsumer UNIQUE_FILTER_INSTANCE = new ListResultsConsumer( UniqueSemantic.FILTER );
	private static final ListResultsConsumer NORMAL_INSTANCE = new ListResultsConsumer( UniqueSemantic.NONE );
	private static final ListResultsConsumer UNIQUE_INSTANCE = new ListResultsConsumer( UniqueSemantic.ASSERT );

	@SuppressWarnings("unchecked")
	public static <R> ListResultsConsumer<R> instance(boolean uniqueFilter, boolean singleResultExpected) {
		if ( singleResultExpected ) {
			return UNIQUE_INSTANCE;
		}
		return uniqueFilter ? UNIQUE_FILTER_INSTANCE : NORMAL_INSTANCE;
	}

	public enum UniqueSemantic {
		NONE,
		FILTER,
		ASSERT;
	}

	private final UniqueSemantic uniqueSemantic;

	public ListResultsConsumer(UniqueSemantic uniqueSemantic) {
		this.uniqueSemantic = uniqueSemantic;
	}

	@Override
	public List<R> consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingStateStandardImpl jdbcValuesSourceProcessingState,
			RowProcessingStateStandardImpl rowProcessingState,
			RowReader<R> rowReader) {
		try {
			final PersistenceContext persistenceContext = session.getPersistenceContext();
			persistenceContext.getLoadContexts().register( jdbcValuesSourceProcessingState );

			final List<R> results = new ArrayList<>();

			if ( uniqueSemantic == UniqueSemantic.NONE ) {
				while ( rowProcessingState.next() ) {
					results.add( rowReader.readRow( rowProcessingState, processingOptions ) );
					rowProcessingState.finishRowProcessing();
				}
			}
			else {
				boolean uniqueRows = false;
				final Class<R> resultJavaType = rowReader.getResultJavaType();
				if ( resultJavaType != null && !resultJavaType.isArray() ) {
					final EntityPersister entityDescriptor = session.getFactory().getMetamodel().findEntityDescriptor(
							resultJavaType );
					if ( entityDescriptor != null ) {
						uniqueRows = true;
					}
				}

				final List<JavaTypeDescriptor> resultJavaTypeDescriptors = rowReader.getResultJavaTypeDescriptors();
				final JavaTypeDescriptor<R> resultJavaTypeDescriptor = resultJavaTypeDescriptors.get( 0 );

				while ( rowProcessingState.next() ) {
					final R row = rowReader.readRow( rowProcessingState, processingOptions );
					boolean add = true;
					if ( uniqueRows ) {
						assert resultJavaTypeDescriptors.size() == 1;
						for ( R existingRow : results ) {
							if ( resultJavaTypeDescriptor.areEqual( existingRow, row ) ) {
								if ( uniqueSemantic == UniqueSemantic.ASSERT && !rowProcessingState.hasCollectionInitializers() ) {
									throw new HibernateException(
											"More than one row with the given identifier was found: " +
													jdbcValuesSourceProcessingState.getExecutionContext()
															.getEntityId() +
													", for class: " +
													resultJavaType.getName()
									);
								}
								add = false;
								break;
							}
						}
					}
					if ( add ) {
						results.add( row );
					}
					rowProcessingState.finishRowProcessing();
				}
			}
			persistenceContext.initializeNonLazyCollections();
			jdbcValuesSourceProcessingState.finishUp();
			return results;
		}
		finally {
			rowReader.finishUp( jdbcValuesSourceProcessingState );
			jdbcValues.finishUp( session );
		}
	}

	@Override
	public boolean canResultsBeCached() {
		return true;
	}
}
