/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.sqm.produce.function.SqmFunctionRegistry;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.DerivedColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;
import org.hibernate.sql.Template;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * A formula is a derived column value
 * @author Gavin King
 */
public class Formula implements MappedColumn, Serializable {
	private static int formulaUniqueInteger;

	private String formula;
	private BasicValue value;
	private SqlTypeDescriptor sqlTypeDescriptor;

	public Formula(String formula) {
		this.formula = formula;
	}

	@Override
	public String getTemplate(Dialect dialect, SqmFunctionRegistry functionRegistry) {
		return Template.renderWhereStringTemplate(formula, dialect, functionRegistry);
	}

	@Override
	public String getText(Dialect dialect) {
		return getFormula();
	}

	@Override
	public String getText() {
		return getFormula();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	public void setSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
	}

	@Override
	public Column generateRuntimeColumn(
			org.hibernate.metamodel.model.relational.spi.Table runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		return new DerivedColumn( runtimeTable, formula, sqlTypeDescriptor );
	}

	public String getFormula() {
		return formula;
	}

	public void setFormula(String string) {
		formula = string;
	}

	@Override
	public boolean isFormula() {
		return true;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "( " + formula + " )";
	}
}
