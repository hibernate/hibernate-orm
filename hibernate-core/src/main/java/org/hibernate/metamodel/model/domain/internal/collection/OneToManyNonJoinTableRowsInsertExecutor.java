/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Chris Cranford
 */
public class OneToManyNonJoinTableRowsInsertExecutor extends OneToManyCreationExecutor {
	public OneToManyNonJoinTableRowsInsertExecutor(
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
		// todo (6.0) - add support for writing the index somehow.
		return super.generateCreationOperation( dmlTableRef, sessionFactory, columnConsumer );
	}
}
