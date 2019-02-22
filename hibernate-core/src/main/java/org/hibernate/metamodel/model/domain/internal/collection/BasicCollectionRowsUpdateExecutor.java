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
import org.hibernate.metamodel.model.domain.spi.CollectionIdentifier;
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
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class BasicCollectionRowsUpdateExecutor implements CollectionRowsUpdateExecutor {
	private static final Logger LOG = Logger.getLogger( BasicCollectionRowsUpdateExecutor.class );

	private final PersistentCollectionDescriptor collectionDescriptor;
	private final Map<Column, JdbcParameter> jdbcParameterMap;
	private final JdbcUpdate jdbcUpdate;

	public BasicCollectionRowsUpdateExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			Table dmlTargetTable,
			boolean hasIndex,
			boolean indexContainsFormula,
			SessionFactoryImplementor sessionFactory) {
		this.collectionDescriptor = collectionDescriptor;
		this.jdbcParameterMap = new HashMap<>();

		final TableReference tableReference = new TableReference( dmlTargetTable, null, false );
		this.jdbcUpdate = generateUpdateOperation( tableReference, jdbcParameterMap::put, hasIndex, indexContainsFormula, sessionFactory );
	}

	@Override
	public void execute(PersistentCollection collection, Object key, SharedSessionContractImplementor session) {

		LOG.infof( "Updating rows of collection: %s#%s", collectionDescriptor.getNavigableRole().getFullPath(), key );

		final List elements = new ArrayList<>();

		final Iterator entries = collection.entries( collectionDescriptor );
		while ( entries.hasNext() ) {
			elements.add( entries.next() );
		}

		final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
		final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

		int count = 0;
		if ( collection.isElementRemoved() ) {
			for ( int i = elements.size() - 1; i >= 0; --i ) {
				final Object entry = elements.get( i );
				if ( collection.needsUpdating( entry, i ) ) {
					bindCollectionKey( key, jdbcParameterBindings, session );
					bindCollectionId( entry, collection, jdbcParameterBindings, session );
					bindCollectionIndex( entry, collection, i, jdbcParameterBindings, session );
					bindCollectionElement( entry, collection,jdbcParameterBindings, session );

					JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( jdbcUpdate, executionContext );
					jdbcParameterBindings.clear();

					count++;
				}
			}
		}
		else {
			for ( int i = 0; i < elements.size(); ++i ) {
				final Object entry = elements.get( i );
				if ( collection.needsUpdating( entry, i ) ) {
					bindCollectionKey( key, jdbcParameterBindings, session );
					bindCollectionId( entry, collection, jdbcParameterBindings, session );
					bindCollectionIndex( entry, collection, i, jdbcParameterBindings, session );
					bindCollectionElement( entry, collection, jdbcParameterBindings, session );

					JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( jdbcUpdate, executionContext );
					jdbcParameterBindings.clear();

					count++;
				}
			}
		}

		LOG.infof( "Done updating rows: %s updated", count );
	}

	protected void bindCollectionId(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		if ( collectionDescriptor.getIdDescriptor() != null ) {
			throw new NotYetImplementedFor6Exception(  );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void bindCollectionKey(
			Object key,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		collectionDescriptor.getCollectionKeyDescriptor().dehydrate(
				collectionDescriptor.getCollectionKeyDescriptor().unresolve( key, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session,
						Clause.UPDATE
				),
				Clause.UPDATE,
				session
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected void bindCollectionIndex(
			Object entry,
			PersistentCollection<?> collection,
			int assumedIndex,
			JdbcParameterBindings jdbcParameterBinding,
			SharedSessionContractImplementor session) {
		if ( collectionDescriptor.getIndexDescriptor() != null ) {
			Object index = collection.getIndex( entry, assumedIndex, collectionDescriptor );
			collectionDescriptor.getIndexDescriptor().dehydrate(
					collectionDescriptor.getIndexDescriptor().unresolve( index, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBinding,
							session,
							Clause.UPDATE
					),
					Clause.UPDATE,
					session
			);
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void bindCollectionElement(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindings jdbcParameterBinding,
			SharedSessionContractImplementor session) {
		final Object element = collection.getElement( entry, collectionDescriptor );
		collectionDescriptor.getElementDescriptor().dehydrate(
				collectionDescriptor.getElementDescriptor().unresolve( element, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBinding,
						session,
						Clause.UPDATE
				),
				Clause.UPDATE,
				session
		);
	}

	@SuppressWarnings("WeakerAccess")
	protected JdbcUpdate generateUpdateOperation(
			TableReference collectionTableRef,
			BiConsumer<Column, JdbcParameter> columnCollector,
			boolean hasIndex,
			boolean indexContainsFormula,
			SessionFactoryImplementor sessionFactory) {
		final AtomicInteger parameterCount = new AtomicInteger();
		final List<Assignment> assignments = new ArrayList<>();

		// ASSIGNMENTS
		// addColumns( elementColumnNames, elementColumnIsSettable, elementColumnWriters )
		//
		// WHERE
		// if ( hasId ) {
		//		addPrimaryKeyColumns( identifierColumnNames )
		// }
		// else if ( hasIndex && !indexContainsFormula ) {
		//		addPrimaryKeyColumns( keyColumnNames, indexColumnNames )
		// }
		// else {
		//		addPrimaryKeyColumns( keyColumnNames )
		//		addPrimaryKeyColumns( elementColumnNames, elementColumnIsInPrimaryKey, elementColumnWriters )
		// }

		// element columns specified as assignments
		final CollectionElement<?> element = collectionDescriptor.getElementDescriptor();
		element.visitColumns(
				(sqlExpressableType, column) -> {
					final ColumnReference columnReference = collectionTableRef.resolveColumnReference( column );
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

		final Junction junction = new Junction( Junction.Nature.CONJUNCTION );

		if ( collectionDescriptor.getIdDescriptor() != null ) {
			CollectionIdentifier identifier = collectionDescriptor.getIdDescriptor();
			identifier.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = collectionTableRef.resolveColumnReference( column );
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.WHERE,
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
					Clause.WHERE,
				sessionFactory.getTypeConfiguration()
			);
		}
		else if ( hasIndex && !indexContainsFormula ) {
			final CollectionIndex<?> index = collectionDescriptor.getIndexDescriptor();
			index.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = collectionTableRef.resolveColumnReference( column );
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.WHERE,
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
					Clause.WHERE,
					sessionFactory.getTypeConfiguration()
			);
			final CollectionKey<?> key = collectionDescriptor.getCollectionKeyDescriptor();
			key.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = collectionTableRef.resolveColumnReference( column );
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.WHERE,
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
					Clause.WHERE,
					sessionFactory.getTypeConfiguration()
			);
		}
		else {
			final CollectionKey<?> key = collectionDescriptor.getCollectionKeyDescriptor();
			key.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = collectionTableRef.resolveColumnReference( column );
						final PositionalParameter parameter = new PositionalParameter(
								parameterCount.getAndIncrement(),
								column.getExpressableType(),
								Clause.WHERE,
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
					Clause.WHERE,
					sessionFactory.getTypeConfiguration()
			);
		}

		return UpdateToJdbcUpdateConverter.createJdbcUpdate(
				new UpdateStatement( collectionTableRef, assignments, junction ),
				sessionFactory
		);
	}

	private void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType sqlExpressableType,
			JdbcParameterBindings jdbcParameterBindings,
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
