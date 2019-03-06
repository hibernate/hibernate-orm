/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionKey;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * Concrete implementation of {@link CollectionRowsUpdateExecutor} specific for One-To-Many associations.
 *
 * @apiNote This implementation delegates to multiple executors which are designed to handle
 * 			the removal and insertion behavior of elements in a one-to-many association when
 * 			the underlying collection is updated.
 *
 * @author Chris Cranford
 */
public class OneToManyRowsUpdateExecutor implements CollectionRowsUpdateExecutor {
	private List<CollectionRowsUpdateExecutor> executors = new ArrayList<>();

	public OneToManyRowsUpdateExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			Table dmlTargetTable,
			boolean rowDeleteEnabled,
			boolean rowInsertEnabled,
			boolean hasIndex,
			boolean indexContainsFormula,
			SessionFactoryImplementor sessionFactory) {
		executors.add(
				new RowsUpdateDeleteExecutor(
						collectionDescriptor,
						dmlTargetTable,
						rowDeleteEnabled,
						hasIndex,
						indexContainsFormula,
						sessionFactory
				)
		);

		executors.add(
				new RowsUpdateInsertExecutor(
						collectionDescriptor,
						dmlTargetTable,
						rowInsertEnabled,
						hasIndex,
						indexContainsFormula,
						sessionFactory
				)
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void execute(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		for ( CollectionRowsUpdateExecutor executor : executors ) {
			executor.execute( collection, key, session );
		}
	}

	/**
	 * Abstract implementation of CollectionRowsUpdateExecutor specific to OneToManyRowsUpdateExecutor.
	 */
	private abstract class AbstractOneToManyRowsUpdateExecutor implements CollectionRowsUpdateExecutor {
		private final PersistentCollectionDescriptor collectionDescriptor;
		private final boolean hasIndex;
		private final boolean indexContainsFormula;
		private final Map<Column, JdbcParameter> jdbcParameterMap;
		private final JdbcUpdate jdbcUpdate;

		AbstractOneToManyRowsUpdateExecutor(
				PersistentCollectionDescriptor collectionDescriptor,
				Table dmlTargetTable,
				boolean hasIndex,
				boolean indexContainsFormula,
				SessionFactoryImplementor sessionFactory) {
			this.collectionDescriptor = collectionDescriptor;
			this.hasIndex = hasIndex;
			this.indexContainsFormula = indexContainsFormula;
			this.jdbcParameterMap = new HashMap<>();

			final TableReference tableReference = new TableReference( dmlTargetTable, null, false );
			this.jdbcUpdate = generateUpdateOperation( tableReference, jdbcParameterMap::put, sessionFactory );
		}

		@Override
		public void execute(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
			if ( isExecutionAllowed() ) {
				final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
				final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

				int i = 0;
				Iterator<?> entries = collection.entries( getCollectionDescriptor() );
				while ( entries.hasNext() ) {
					Object entry = entries.next();
					if ( collection.needsUpdating( entry, i ) ) {
						bindCollectionKey( key, jdbcParameterBindings, session, Clause.UPDATE );
						bindCollectionIndex( entry, i, collection, jdbcParameterBindings, session, Clause.UPDATE );
						bindCollectionElement( entry, i, collection, jdbcParameterBindings, session, Clause.UPDATE );

						JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( jdbcUpdate, executionContext );
						jdbcParameterBindings.clear();
					}
					i++;
				}
			}
		}

		protected PersistentCollectionDescriptor getCollectionDescriptor() {
			return collectionDescriptor;
		}

		protected boolean hasIndex() {
			return hasIndex;
		}

		protected boolean indexContainsFormula() {
			return indexContainsFormula;
		}

		/**
		 * Determines whether the executor is allowed to execute on the collection.
		 */
		protected abstract boolean isExecutionAllowed();

		protected abstract JdbcUpdate generateUpdateOperation(
				TableReference tableReference,
				BiConsumer<Column, JdbcParameter> columnCollector,
				SessionFactoryImplementor sessionFactory);

		@SuppressWarnings("WeakerAccess")
		protected void bindCollectionKey(
				Object key,
				JdbcParameterBindingsImpl jdbcParameterBindings,
				SharedSessionContractImplementor session,
				Clause clause) {
			collectionDescriptor.getCollectionKeyDescriptor().dehydrate(
					collectionDescriptor.getCollectionKeyDescriptor().unresolve( key, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBindings,
							session,
							clause
					),
					clause,
					session
			);
		}

		@SuppressWarnings("WeakerAccess")
		protected void bindCollectionIndex(
				Object entry,
				int assumedIndex,
				PersistentCollection collection,
				JdbcParameterBindingsImpl jdbcParameterBindings,
				SharedSessionContractImplementor session,
				Clause clause) {
			final CollectionIndex indexDescriptor = collectionDescriptor.getIndexDescriptor();
			if ( indexDescriptor != null ) {
				Object index = collection.getIndex( entry, assumedIndex, collectionDescriptor );
				if ( indexDescriptor.getBaseIndex() != 0 ) {
					index = (Integer) index + indexDescriptor.getBaseIndex();
				}
				indexDescriptor.dehydrate(
						indexDescriptor.unresolve( index, session ),
						(jdbcValue, type, boundColumn) -> createBinding(
								jdbcValue,
								boundColumn,
								type,
								jdbcParameterBindings,
								session,
								clause
						),
						clause,
						session
				);
			}
		}

		@SuppressWarnings({"unchecked"})
		protected void bindCollectionElement(
				Object entry,
				int assumedIndex,
				PersistentCollection collection,
				JdbcParameterBindingsImpl jdbcParameterBindings,
				SharedSessionContractImplementor session,
				Clause clause) {
			final Object element = collection.getElement( entry, collectionDescriptor );
			getCollectionDescriptor().getElementDescriptor().dehydrate(
					getCollectionDescriptor().getElementDescriptor().unresolve( element, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBindings,
							session,
							clause
					),
					clause,
					session
			);
		}

		void createBinding(
				Object jdbcValue,
				Column boundColumn,
				SqlExpressableType sqlExpressableType,
				JdbcParameterBindingsImpl jdbcParameterBindings,
				SharedSessionContractImplementor session,
				Clause clause) {
			final JdbcParameter jdbcParameter = resolveJdbcParameter( boundColumn );

			jdbcParameterBindings.addBinding(
					jdbcParameter,
					new LiteralParameter(
							jdbcValue,
							sqlExpressableType,
							clause,
							session.getFactory().getTypeConfiguration()
					)
			);
		}

		private JdbcParameter resolveJdbcParameter(Column boundColumn) {
			final JdbcParameter jdbcParameter = jdbcParameterMap.get( boundColumn );
			if ( jdbcParameter == null ) {
				throw new IllegalStateException( "JdbcParameter not found for Column [" + boundColumn + "]" );
			}
			return jdbcParameter;
		}
	}

	/**
	 * Implementation that takes care of remove-specific operations for One-To-Many.
	 *
	 */
	private class RowsUpdateDeleteExecutor extends AbstractOneToManyRowsUpdateExecutor {
		private final boolean isRowDeleteEnabled;

		RowsUpdateDeleteExecutor(
				PersistentCollectionDescriptor persistentCollectionDescriptor,
				Table dmlTargetTable,
				boolean rowDeleteEnabled,
				boolean hasIndex,
				boolean indexContainsFormula,
				SessionFactoryImplementor sessionFactory) {
			super( persistentCollectionDescriptor, dmlTargetTable, hasIndex, indexContainsFormula, sessionFactory );
			this.isRowDeleteEnabled = rowDeleteEnabled;
		}

		@Override
		protected boolean isExecutionAllowed() {
			return isRowDeleteEnabled;
		}

		@Override
		protected JdbcUpdate generateUpdateOperation(
				TableReference dmlTableRef,
				BiConsumer<Column, JdbcParameter> columnCollector,
				SessionFactoryImplementor sessionFactory) {
			final AtomicInteger parameterCount = new AtomicInteger();
			final List<Assignment> assignments = new ArrayList<>();

			final CollectionKey<?> collectionKey = getCollectionDescriptor().getCollectionKeyDescriptor();
			collectionKey.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

						final LiteralParameter parameter = new LiteralParameter(
								null,
								column.getExpressableType(),
								Clause.UPDATE,
								sessionFactory.getTypeConfiguration()
						);

						assignments.add( new Assignment( columnReference, parameter ) );
					},
					Clause.UPDATE,
					sessionFactory.getTypeConfiguration()
			);

			if ( getCollectionDescriptor().getIndexDescriptor() != null ) {
				if ( hasIndex() && !indexContainsFormula() ) {
					final CollectionIndex<?> collectionIndex = getCollectionDescriptor().getIndexDescriptor();
					collectionIndex.visitColumns(
							(sqlExpressableType, column) -> {
								final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

								final LiteralParameter parameter = new LiteralParameter(
										null,
										column.getExpressableType(),
										Clause.UPDATE,
										sessionFactory.getTypeConfiguration()
								);

								assignments.add( new Assignment( columnReference, parameter ) );
							},
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);
				}
			}

			Junction junction = new Junction( Junction.Nature.CONJUNCTION );

			collectionKey.visitColumns(
					(sqlExpressableType, column) -> {
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.UPDATE,
								sessionFactory.getTypeConfiguration()
						);

						columnCollector.accept( column, parameter );

						junction.add(
								new ComparisonPredicate(
										new ColumnReference( column ),
										ComparisonOperator.EQUAL,
										parameter
								)
						);
					},
					Clause.UPDATE,
					sessionFactory.getTypeConfiguration()
			);

			final CollectionElement<?> collectionElement = getCollectionDescriptor().getElementDescriptor();
			collectionElement.visitColumns(
					(sqlExpressableType, column) -> {
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.UPDATE,
								sessionFactory.getTypeConfiguration()
						);

						columnCollector.accept( column, parameter );

						junction.add(
								new ComparisonPredicate(
										new ColumnReference( column ),
										ComparisonOperator.EQUAL,
										parameter
								)
						);
					},
					Clause.UPDATE,
					sessionFactory.getTypeConfiguration()
			);

			return UpdateToJdbcUpdateConverter.createJdbcUpdate(
					new UpdateStatement( dmlTableRef, assignments, junction ),
					sessionFactory
			);
		}

		@Override
		protected void bindCollectionIndex(
				Object entry,
				int assumedIndex,
				PersistentCollection collection,
				JdbcParameterBindingsImpl jdbcParameterBindings,
				SharedSessionContractImplementor session,
				Clause clause) {
			// never writes the index for this scenario.
		}

		@Override
		protected void bindCollectionElement(
				Object entry,
				int assumedIndex,
				PersistentCollection collection,
				JdbcParameterBindingsImpl jdbcParameterBindings,
				SharedSessionContractImplementor session,
				Clause clause) {
			final Object element = collection.getSnapshotElement( entry, assumedIndex );
			getCollectionDescriptor().getElementDescriptor().dehydrate(
					getCollectionDescriptor().getElementDescriptor().unresolve( element, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBindings,
							session,
							clause
					),
					clause,
					session
			);
		}
	}

	/**
	 * Implementation that takes care of insert-specific operations for One-To-Many
	 */
	private class RowsUpdateInsertExecutor extends AbstractOneToManyRowsUpdateExecutor {
		private final boolean isRowInsertEnabled;

		RowsUpdateInsertExecutor(
				PersistentCollectionDescriptor persistentCollectionDescriptor,
				Table dmlTargetTable,
				boolean rowInsertEnabled,
				boolean hasIndex,
				boolean indexContainsFormula,
				SessionFactoryImplementor sessionFactory) {
			super( persistentCollectionDescriptor, dmlTargetTable, hasIndex, indexContainsFormula, sessionFactory );
			this.isRowInsertEnabled = rowInsertEnabled;
		}

		@Override
		protected boolean isExecutionAllowed() {
			return isRowInsertEnabled;
		}

		@Override
		protected JdbcUpdate generateUpdateOperation(
				TableReference dmlTableRef,
				BiConsumer<Column, JdbcParameter> columnCollector,
				SessionFactoryImplementor sessionFactory) {

			final AtomicInteger parameterCount = new AtomicInteger();
			final List<Assignment> assignments = new ArrayList<>();

			final CollectionKey<?> collectionKey = getCollectionDescriptor().getCollectionKeyDescriptor();
			collectionKey.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.UPDATE,
								sessionFactory.getTypeConfiguration()
						);

						columnCollector.accept( column, parameter );

						assignments.add( new Assignment( columnReference, parameter ) );
					},
					Clause.UPDATE,
					sessionFactory.getTypeConfiguration()
			);

			if ( getCollectionDescriptor().getIdDescriptor() != null ) {
				throw new NotYetImplementedFor6Exception(  );
			}


			if ( getCollectionDescriptor().getIndexDescriptor() != null ) {
				if ( hasIndex() && !indexContainsFormula() ) {
					final CollectionIndex<?> collectionIndex = getCollectionDescriptor().getIndexDescriptor();
					collectionIndex.visitColumns(
							(sqlExpressableType, column) -> {
								final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

								final PositionalParameter parameter = new PositionalParameter(
										parameterCount.getAndIncrement(),
										column.getExpressableType(),
										Clause.UPDATE,
										sessionFactory.getTypeConfiguration()
								);

								columnCollector.accept( column, parameter );

								assignments.add( new Assignment( columnReference, parameter ) );
							},
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);
				}
			}

			Junction junction = new Junction( Junction.Nature.CONJUNCTION );

			final CollectionElement<?> collectionElement = getCollectionDescriptor().getElementDescriptor();
			collectionElement.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.UPDATE,
								sessionFactory.getTypeConfiguration()
						);

						columnCollector.accept( column, parameter );

						junction.add(
								new ComparisonPredicate(
										columnReference,
										ComparisonOperator.EQUAL,
										parameter
								)
						);
					},
					Clause.UPDATE,
					sessionFactory.getTypeConfiguration()
			);

			return UpdateToJdbcUpdateConverter.createJdbcUpdate(
					new UpdateStatement( dmlTableRef, assignments, junction ),
					sessionFactory
			);
		}
	}
}

