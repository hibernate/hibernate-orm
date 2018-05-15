/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupResolver;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

/**
 * @author Steve Ebersole
 */
public interface TableGroupProducer {
	TableGroup buildTableGroup(
			SqmFrom fromElement,
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			TableGroupResolver tableGroupResolver);
}
