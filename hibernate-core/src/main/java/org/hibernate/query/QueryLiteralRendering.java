/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.util.Locale;

import org.hibernate.query.criteria.LiteralHandlingMode;

/**
 * Defines possible ways we can handle query literals in terms of handling in regards to JDBC.
 *
 * @author Steve Ebersole
 */
public enum QueryLiteralRendering {

	AUTO( "auto" ),
	/**
	 * Always render as a SQL literal, never a parameter
	 */
	AS_LITERAL( "literal" ),
	/**
	 * Always as a parameter, never as a literal
	 */
	AS_PARAM( "param" ),
	/**
	 * As a parameter when the literal occurs outside the SELECT clause,
	 * otherwise render as a SQL literal.
	 */
	AS_PARAM_OUTSIDE_SELECT( "param-outside-select" );

	private final String externalForm;

	QueryLiteralRendering(String externalForm) {
		this.externalForm = externalForm;
	}

	public String toExternalForm() {
		return externalForm;
	}

	public static QueryLiteralRendering fromExternalForm(Object externalValue) {
		if ( externalValue != null ) {

			if ( externalValue instanceof QueryLiteralRendering ) {
				return (QueryLiteralRendering) externalValue;
			}

			if ( externalValue instanceof LiteralHandlingMode ) {
				return ( (LiteralHandlingMode) externalValue ).getCounterpart();
			}

			final String externalValueString = externalValue.toString().trim().toLowerCase( Locale.ROOT );
			if ( externalValueString.isEmpty() ) {
				return null;
			}

			if ( AS_LITERAL.externalForm.equals( externalValueString ) ) {
				return AS_LITERAL;
			}

			if ( AS_PARAM.externalForm.equals( externalValueString ) ) {
				return AS_PARAM;
			}

			if ( AS_PARAM_OUTSIDE_SELECT.externalForm.equals( externalValueString ) ) {
				return AS_PARAM_OUTSIDE_SELECT;
			}

			try {
				final LiteralHandlingMode legacy = LiteralHandlingMode.interpret( externalValue );
				return legacy.getCounterpart();
			}
			catch (Exception ignore) {
			}
		}

		// the default...
		return AS_PARAM_OUTSIDE_SELECT;
	}
}
