/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.sql.ast.produce.result.spi.ColumnReferenceResolver;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * Defines a "source" for ColumnBindings related to a DomainReference (relative
 * to this source).
 *
 * @author Steve Ebersole
 */
public interface ColumnReferenceSource extends ColumnReferenceResolver {
	TableGroup getTableGroup();

	TableReference locateTableReference(Table table);
	ColumnReference resolveColumnReference(Column column);
}
