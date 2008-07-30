/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
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
	private static int formulaUniqueInteger=0;

	private String formula;
	private int uniqueInteger;

	public Formula() {
		uniqueInteger = formulaUniqueInteger++;
	}

	public String getTemplate(Dialect dialect, SQLFunctionRegistry functionRegistry) {
		return Template.renderWhereStringTemplate(formula, dialect, functionRegistry);
	}
	public String getText(Dialect dialect) {
		return getFormula();
	}
	public String getText() {
		return getFormula();
	}
	public String getAlias(Dialect dialect) {
		return "formula" + Integer.toString(uniqueInteger) + '_';
	}
	public String getAlias(Dialect dialect, Table table) {
		return getAlias(dialect);
	}
	public String getFormula() {
		return formula;
	}
	public void setFormula(String string) {
		formula = string;
	}
	public boolean isFormula() {
		return true;
	}

	public String toString() {
		return this.getClass().getName() + "( " + formula + " )";
	}
}
