/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.relational;

import java.util.function.Supplier;

import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

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

	void setSqlTypeDescriptorAccess(Supplier<SqlTypeDescriptor> sqlTypeDescriptorAccess);

	SqlTypeDescriptor getSqlTypeDescriptor();

	void setJavaTypeMapping(JavaTypeMapping javaTypeMapping);

	JavaTypeMapping getJavaTypeMapping();

	Column generateRuntimeColumn(
			Table runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment,
			TypeConfiguration typeConfiguration);

	boolean isFormula();

	String getTemplate(Dialect dialect, SqmFunctionRegistry functionRegistry);
}
