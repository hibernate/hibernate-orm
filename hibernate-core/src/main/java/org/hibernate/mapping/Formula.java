/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.Dialect;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.StringHelper.replace;
import static org.hibernate.internal.util.StringHelper.safeInterning;
import static org.hibernate.sql.Template.TEMPLATE;
import static org.hibernate.sql.Template.renderWhereStringTemplate;

/**
 * A mapping model object representing a SQL {@linkplain org.hibernate.annotations.Formula formula}
 * which is used as a "derived" {@link Column} in an entity mapping.
 *
 * @author Gavin King
 */
public final class Formula implements Selectable, Serializable {

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
	public String getTemplate(Dialect dialect, TypeConfiguration typeConfiguration) {
		final String template = renderWhereStringTemplate( formula, dialect, typeConfiguration );
		return safeInterning( replace( template, "{alias}", TEMPLATE ) );
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
	public String getCustomReadExpression() {
		return getFormula();
	}

	@Override
	public String getCustomWriteExpression() {
		return null;
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
		return getClass().getSimpleName() + "( " + formula + " )";
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Formula other
			&& formula.equals( other.formula );
	}

	@Override
	public int hashCode() {
		return formula.hashCode();
	}
}
