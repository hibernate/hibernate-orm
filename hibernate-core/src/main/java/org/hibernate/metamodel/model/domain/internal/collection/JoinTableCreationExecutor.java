/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.InsertToJdbcInsertConverter;
import org.hibernate.sql.ast.tree.spi.InsertStatement;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * CollectionCreationExecutor impl for collections with a join table
 * (basic, embedded and entity-with-join-table)
 *
 * @author Steve Ebersole
 */
public class JoinTableCreationExecutor extends AbstractCreationExecutor {
	public JoinTableCreationExecutor(
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
		final InsertStatement  insertStatement = new InsertStatement( dmlTableRef );

		final AtomicInteger parameterCount = new AtomicInteger(  );

		applyNavigable( getCollectionDescriptor().getCollectionKeyDescriptor(), insertStatement, parameterCount, columnConsumer, sessionFactory );

		if ( getCollectionDescriptor().getIdDescriptor() != null ) {
			applyNavigable( getCollectionDescriptor().getIdDescriptor(), insertStatement, parameterCount, columnConsumer, sessionFactory );
		}

		if ( getCollectionDescriptor().getIndexDescriptor() != null ) {
			applyNavigable( getCollectionDescriptor().getIndexDescriptor(), insertStatement, parameterCount, columnConsumer, sessionFactory );
		}

		applyNavigable( getCollectionDescriptor().getElementDescriptor(), insertStatement, parameterCount, columnConsumer, sessionFactory );

		return InsertToJdbcInsertConverter.createJdbcInsert( insertStatement, sessionFactory );
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyNavigable(
			Navigable navigable,
			InsertStatement insertStatement,
			AtomicInteger parameterCount,
			BiConsumer<Column, JdbcParameter> columnConsumer,
			SessionFactoryImplementor sessionFactory) {
		//noinspection RedundantCast
		navigable.visitColumns(
				(BiConsumer<SqlExpressableType, Column>) (sqlExpressableType, column) -> {
					insertStatement.addTargetColumnReference(
							insertStatement.getTargetTable().resolveColumnReference( column )
					);

					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.INSERT,
							sessionFactory.getTypeConfiguration()
					);

					insertStatement.addValue( parameter );

					columnConsumer.accept( column, parameter );
				},
				Clause.INSERT,
				sessionFactory.getTypeConfiguration()
		);
	}
}
