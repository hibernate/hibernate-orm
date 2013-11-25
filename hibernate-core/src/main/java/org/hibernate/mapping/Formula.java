/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.mapping;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.sql.Template;

/**
 * A formula is a derived column value
 * @author Gavin King
 */
public class Formula implements Selectable, Serializable {
	private static int formulaUniqueInteger;

	private String formula;
	private int uniqueInteger;

	public Formula() {
		uniqueInteger = formulaUniqueInteger++;
	}

	@Override
	public String getTemplate(Dialect dialect, SQLFunctionRegistry functionRegistry) {
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
	public String getAlias(Dialect dialect) {
		return "formula" + Integer.toString(uniqueInteger) + '_';
	}

	@Override
	public String getAlias(Dialect dialect, Table table) {
		return getAlias(dialect);
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
