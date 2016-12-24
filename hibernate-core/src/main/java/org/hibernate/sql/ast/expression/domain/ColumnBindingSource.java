/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.domain;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Table;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.from.TableBinding;
import org.hibernate.sql.ast.from.TableGroup;

/**
 * Defines a "source" for ColumnBindings related to a DomainReference (relative
 * to this source).
 *
 * @author Steve Ebersole
 */
public interface ColumnBindingSource {
	TableGroup getTableGroup();

	TableBinding locateTableBinding(Table table);
	ColumnBinding resolveColumnBinding(Column column);
}
