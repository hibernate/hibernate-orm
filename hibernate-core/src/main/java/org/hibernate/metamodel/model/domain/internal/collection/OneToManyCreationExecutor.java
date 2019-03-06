/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.Navigable;
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
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcParameter;


/**
 * @author Steve Ebersole
 */
public class OneToManyCreationExecutor extends AbstractCreationExecutor {
	public OneToManyCreationExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			Table dmlTargetTable,
			SessionFactoryImplementor sessionFactory) {
		super( collectionDescriptor, dmlTargetTable, sessionFactory );
	}

	@Override
	protected JdbcMutation generateCreationOperation(
			TableReference dmlTableRef,
			SessionFactoryImplementor sessionFactory,
			BiConsumer<Column, JdbcParameter> columnConsumer) {

		final List<Assignment> assignments = new ArrayList<>();
		final AtomicInteger parameterCount = new AtomicInteger();

		applyNavigable(
				getCollectionDescriptor().getCollectionKeyDescriptor(),
				dmlTableRef,
				parameterCount,
				columnConsumer,
				assignments,
				sessionFactory
		);

		if ( getCollectionDescriptor().getIndexDescriptor() != null ) {
			applyNavigable(
					getCollectionDescriptor().getIndexDescriptor(),
					dmlTableRef,
					parameterCount,
					columnConsumer,
					assignments,
					sessionFactory
			);
		}

		final Predicate predicate = resolvePredicate(
				getCollectionDescriptor().getElementDescriptor(),
				dmlTableRef,
				parameterCount,
				columnConsumer,
				sessionFactory
		);

		final UpdateStatement updateStatement = new UpdateStatement( dmlTableRef, assignments, predicate );

		return UpdateToJdbcUpdateConverter.createJdbcUpdate( updateStatement, sessionFactory );
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyNavigable(
			Navigable navigable,
			TableReference dmlTableRef,
			AtomicInteger parameterCount,
			BiConsumer<Column, JdbcParameter> columnConsumer,
			List<Assignment> assignments,
			SessionFactoryImplementor sessionFactory) {
		//noinspection RedundantCast
		navigable.visitColumns(
				(BiConsumer<SqlExpressableType, Column>) (sqlExpressableType, column) -> {

					final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.INSERT,
							sessionFactory.getTypeConfiguration()
					);

					final Assignment assignment = new Assignment( columnReference, parameter );
					assignments.add( assignment );

					columnConsumer.accept( column, parameter );
				},
				Clause.UPDATE,
				sessionFactory.getTypeConfiguration()
		);
	}

	private Predicate resolvePredicate(
			Navigable<?> navigable,
			TableReference dmltableRef,
			AtomicInteger parameterCount,
			BiConsumer<Column, JdbcParameter> columnConsumer,
			SessionFactoryImplementor sessionFactory) {
		Junction junction = new Junction( Junction.Nature.CONJUNCTION );
		navigable.visitColumns(
				(sqlExpressableType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);

					columnConsumer.accept( column, parameter );

					junction.add(
							new ComparisonPredicate(
									new ColumnReference( column ),
									ComparisonOperator.EQUAL,
									parameter
							)
					);
				},
				Clause.WHERE,
				sessionFactory.getTypeConfiguration()
		);
		return junction;
	}

	@Override
	protected void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType type,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final JdbcParameter jdbcParameter = resolveJdbcParameter( boundColumn );

		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new LiteralParameter(
						jdbcValue,
						type,
						Clause.UPDATE,
						session.getFactory().getTypeConfiguration()
				)
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void bindCollectionElement(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final Object element = collection.getElement( entry, getCollectionDescriptor() );
		getCollectionDescriptor().getElementDescriptor().dehydrate(
				getCollectionDescriptor().getElementDescriptor().unresolve( element, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.WHERE,
				session
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void bindCollectionId(
			Object entry,
			int assumedIdentifier,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		if ( getCollectionDescriptor().getIdDescriptor() != null ) {
			final Object id = collection.getIdentifier( entry, assumedIdentifier, getCollectionDescriptor() );
			getCollectionDescriptor().getIdDescriptor().dehydrate(
					getCollectionDescriptor().getIdDescriptor().unresolve( id, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBindings,
							session
					),
					Clause.UPDATE,
					session
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void bindCollectionIndex(
			Object entry,
			int assumedIndex,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final CollectionIndex indexDescriptor = getCollectionDescriptor().getIndexDescriptor();
		if ( indexDescriptor != null ) {
			Object index = collection.getIndex( entry, assumedIndex, getCollectionDescriptor() );
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
							session
					),
					Clause.UPDATE,
					session

			);
		}
	}

	@Override
	protected void bindCollectionKey(
			Object key,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		getCollectionDescriptor().getCollectionKeyDescriptor().dehydrate(
				getCollectionDescriptor().getCollectionKeyDescriptor().unresolve( key, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.UPDATE,
				session
		);
	}
}
