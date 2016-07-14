/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.persister.common.spi.AbstractTable;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.common.spi.SqmTypeImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.sqm.ast.from.AbstractTableGroup;
import org.hibernate.sql.sqm.ast.from.EntityTableGroup;
import org.hibernate.sql.sqm.ast.from.TableSpace;
import org.hibernate.sql.sqm.convert.internal.FromClauseIndex;
import org.hibernate.sql.sqm.convert.internal.SqlAliasBaseManager;
import org.hibernate.sqm.domain.EntityType;
import org.hibernate.sqm.query.JoinType;
import org.hibernate.sqm.query.from.FromElement;

/**
 * Isolate things we think are involved in an "improved design" for EntityPersister.
 *
 * @author Steve Ebersole
 */
public interface ImprovedEntityPersister extends EntityType, IdentifiableTypeImplementor, SqmTypeImplementor {
	/**
	 * In integrating this upstream, the methods here would all be part of EntityPersister
	 * but here we cannot do that and therefore still need access to EntityPersister
	 *
	 * @return The ORM EntityPersister
	 */
	EntityPersister getEntityPersister();

	@Override
	IdentifierDescriptorImplementor getIdentifierDescriptor();

	AbstractTable getRootTable();

	EntityTableGroup buildTableGroup(
			FromElement fromElement,
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseIndex fromClauseIndex);

	void addTableJoins(AbstractTableGroup group, JoinType joinType, Column[] fkColumns, Column[] fkTargetColumns);
}
