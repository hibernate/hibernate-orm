/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.from;

import java.util.List;

import org.hibernate.persister.common.spi.SingularAttributeImplementor;
import org.hibernate.sql.sqm.ast.expression.AttributeReference;
import org.hibernate.sql.sqm.ast.expression.EntityReference;

/**
 * Group together related {@link TableBinding} references (generally related by EntityPersister or CollectionPersister),
 *
 * @author Steve Ebersole
 */
public interface TableGroup {
	TableSpace getTableSpace();
	String getAliasBase();
	TableBinding getRootTableBinding();
	List<TableJoin> getTableJoins();

	ColumnBinding[] resolveBindings(SingularAttributeImplementor attribute);
	AttributeReference resolve(SingularAttributeImplementor attribute);

	EntityReference resolveEntityReference();
}
