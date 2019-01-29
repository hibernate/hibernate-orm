/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.tree.spi.DeleteStatement;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.exec.spi.JdbcParameter;

/**
 * @author Chris Cranford
 */
public class OneToManyRowsDeletionExecutor extends AbstractCollectionRowsDeletionExecutor {
	public OneToManyRowsDeletionExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory,
			Table dmlTargetTable,
			boolean deleteByIndex) {
		super( collectionDescriptor, sessionFactory, dmlTargetTable, deleteByIndex );
	}

	@Override
	protected DeleteStatement generateRowsDeletionOperation(
			TableReference collectionTableRef,
			SessionFactoryImplementor sessionFactory,
			BiConsumer<Column, JdbcParameter> parameterCollector) {

		final AtomicInteger parameterCount = new AtomicInteger();
		final Junction deleteRestriction = new Junction( Junction.Nature.CONJUNCTION );

		applyNavigablePredicate(
				getCollectionDescriptor().getCollectionKeyDescriptor(),
				collectionTableRef,
				parameterCount,
				parameterCollector,
				deleteRestriction::add,
				sessionFactory
		);

		if ( getCollectionDescriptor().getIdDescriptor() == null ) {
			if ( isDeleteByIndex() ) {
				applyNavigablePredicate(
						getCollectionDescriptor().getIndexDescriptor(),
						collectionTableRef,
						parameterCount,
						parameterCollector,
						deleteRestriction::add,
						sessionFactory
				);
			}
			else {
				applyNavigablePredicate(
						getCollectionDescriptor().getElementDescriptor(),
						collectionTableRef,
						parameterCount,
						parameterCollector,
						deleteRestriction::add,
						sessionFactory
				);
			}
		}

		return new DeleteStatement( collectionTableRef, deleteRestriction );
	}
}
