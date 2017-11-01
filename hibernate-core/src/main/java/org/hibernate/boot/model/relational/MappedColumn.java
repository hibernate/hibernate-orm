/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Models any mapped "selectable reference".  This might be a reference to a
 * physical column or a derived value (formula)
 *
 * @author Steve Ebersole
 */
public interface MappedColumn extends Selectable {
	/**
	 * The column text.  For a physical column, this would be its name.  For
	 * a derived columns, this would be the formula expression.
	 */
	String getText();

	SqlTypeDescriptor getSqlTypeDescriptor();

	Column generateRuntimeColumn(
			Table runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment);

	// todo (6.0) : others as deemed appropriate - see o.h.mapping.Selectable
}
