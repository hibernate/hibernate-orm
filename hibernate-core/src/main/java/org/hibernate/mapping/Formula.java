/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.sql.Template;

import static org.hibernate.internal.util.StringHelper.safeInterning;

/**
 * A formula is a derived column value
 * @author Gavin King
 */
public class Formula implements Selectable, Serializable {

	private static final AtomicInteger formulaUniqueInteger = new AtomicInteger();

	private String formula;
	private final int uniqueInteger;

	public Formula() {
		uniqueInteger = formulaUniqueInteger.incrementAndGet();
	}

	public Formula(String formula) {
		this();
		this.formula = formula;
	}

	@Override
	public String getTemplate(Dialect dialect, SQLFunctionRegistry functionRegistry) {
		String template = Template.renderWhereStringTemplate(formula, dialect, functionRegistry);
		return safeInterning( StringHelper.replace( template, "{alias}", Template.TEMPLATE ) );
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
		return "formula" + AliasConstantsHelper.get( uniqueInteger );
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
