/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.persister.model.relational.spi.Column;
import org.hibernate.persister.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.persister.model.relational.spi.Table;

/**
 * Models any mapped "selectable reference".  This might be a reference to a
 * physical column or a derived value (formula)
 *
 * @author Steve Ebersole
 */
public interface MappedColumn {
	/**
	 * The column text.  For a physical column, this would be its name.  For
	 * a derived columns, this would be the formula expression.
	 */
	String getText();

	Column generateRuntimeColumn(
			Table runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment);

	// todo (6.0) : others as deemed appropriate - see o.h.mapping.Selectable
}
