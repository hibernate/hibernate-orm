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
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.LoadParameterBindingContext;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Chris Cranford
 */
public class OneToManyRowsIndexUpdateExecutor implements CollectionRowsIndexUpdateExecutor {
	private final PersistentCollectionDescriptor collectionDescriptor;
	private final Map<Column, JdbcParameter> jdbcParameterMap;
	private final JdbcMutation jdbcMutation;

	public OneToManyRowsIndexUpdateExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			Table dmlTargetTable,
			SessionFactoryImplementor sessionFactory) {
		this.collectionDescriptor = collectionDescriptor;
		this.jdbcParameterMap = new HashMap<>();

		TableReference tableReference = new TableReference( dmlTargetTable, null, false );
		this.jdbcMutation = generateUpdateMutation( tableReference, jdbcParameterMap::put, sessionFactory );
	}

	@Override
	public void execute(
			PersistentCollection collection,
			Object key,
			boolean queuedOperations,
			boolean resetIndex,
			SharedSessionContractImplementor session) {

		Iterator<?> entries = resolveEntries( collection, queuedOperations );
		if ( entries.hasNext() ) {
			final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
			final BasicExecutionContext executionContext = new BasicExecutionContext(
					session,
					new LoadParameterBindingContext( session.getSessionFactory(), key )
			);

			int nextIndex = resetIndex ? 0 : collectionDescriptor.getSize( key, session );
			while ( entries.hasNext() ) {
				final Object entry = entries.next();
				if ( entry != null && collection.entryExists( entry, nextIndex ) ) {
					bindCollectionKey( key, jdbcParameterBindings, session );
					bindCollectionId( entry, nextIndex, collection, jdbcParameterBindings, session );
					bindCollectionIndex( entry, nextIndex, collection, jdbcParameterBindings, session );
					bindCollectionElement( entry, collection, jdbcParameterBindings, session );

					JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( jdbcMutation, jdbcParameterBindings, executionContext );
				}

				nextIndex++;
				jdbcParameterBindings.clear();
			}
		}
	}

	private Iterator<?> resolveEntries(PersistentCollection collection, boolean queuedOperations) {
		if ( queuedOperations ) {
			return collection.queuedAdditionIterator();
		}
		else {
			return collection.entries( collectionDescriptor );
		}
	}

	private JdbcUpdate generateUpdateMutation(
			TableReference tableRef,
			BiConsumer<Column, JdbcParameter> columnCollector,
			SessionFactoryImplementor sessionFactory) {
		final AtomicInteger parameterCount = new AtomicInteger();
		final List<Assignment> assignments = new ArrayList<>();
		final Junction junction = new Junction( Junction.Nature.CONJUNCTION );

		// WHERE elementColumnNames, elementColumnIsSettable, elementColumnWriters, identifierColumnName
		//   SET indexColumNames

		final CollectionIndex<?> collectionIndex = collectionDescriptor.getIndexDescriptor();
		collectionIndex.visitColumns(
				(sqlExpressableType, column) -> {
					final ColumnReference columnReference = tableRef.resolveColumnReference( column );

					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);

					columnCollector.accept( column, parameter );
					assignments.add( new Assignment( columnReference, parameter) );
				},
				Clause.UPDATE,
				sessionFactory.getTypeConfiguration()
		);

		if ( collectionDescriptor.getIdDescriptor() != null ) {
			final CollectionIdentifier identifier = collectionDescriptor.getIdDescriptor();
			identifier.visitColumns(
					(sqlExpressableType, column) -> {
						final ColumnReference columnReference = tableRef.resolveColumnReference( column );

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
					Clause.UPDATE,
					sessionFactory.getTypeConfiguration()
			);
		}

		// todo (6.0) - this is currently somewhat of a hack
		//		When calling visitColumns on the CollectionElementEntityImpl, because this scenario is in
		//		fact an inverse collection, the method is a no-op and therefore no columns are propagated
		//		for the where clause, which must be the actual collection element's PK.  For now, unwrap
		//		the collection element and apply the logic to the identifier directly.
		final CollectionElement<?> element = collectionDescriptor.getElementDescriptor();

		final EntityValuedNavigable navigable = (EntityValuedNavigable) element;
		final EntityIdentifier<?,?> identifier = navigable.getEntityDescriptor().getIdentifierDescriptor();
		identifier.visitColumns(
				(sqlExpressableType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.WHERE,
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
				new UpdateStatement( tableRef, assignments, junction ),
				sessionFactory
		);
	}

	private void bindCollectionKey(
			Object key,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		// do nothing?
	}

	private void bindCollectionId(
			Object entry,
			int assumedIndex,
			PersistentCollection collection,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		if ( collectionDescriptor.getIdDescriptor() != null ) {
			throw new NotYetImplementedFor6Exception(  );
		}
	}

	private void bindCollectionIndex(
			Object entry,
			int assumedIndex,
			PersistentCollection collection,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final CollectionIndex<?> collectionIndex = collectionDescriptor.getIndexDescriptor();
		Object index = collection.getIndex( entry, assumedIndex, collectionDescriptor );
		if ( collectionIndex.getBaseIndex() != 0 ) {
			index = (Integer) index + collectionDescriptor.getIndexDescriptor().getBaseIndex();
		}
		collectionIndex.dehydrate(
				collectionIndex.unresolve( index, session ),
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

	private void bindCollectionElement(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindings jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final Object element = collection.getElement( entry, collectionDescriptor );
		final CollectionElement<?> collectionElement = collectionDescriptor.getElementDescriptor();
		collectionElement.dehydrate(
				collectionElement.unresolve( element, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session,
						Clause.WHERE
				),
				Clause.UPDATE,
				session
		);
	}

	void createBinding(
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
