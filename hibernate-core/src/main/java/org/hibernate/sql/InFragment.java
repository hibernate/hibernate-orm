/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.internal.util.StringHelper;

/**
 * A SQL {@code IN} expression.
 *
 * @author Gavin King
 */
@Internal
public class InFragment {

	public static final String NULL = "null";
	public static final String NOT_NULL = "not null";

	protected String columnName;
	protected List<Object> values = new ArrayList<>();

	/**
	 * @param value an SQL literal, NULL, or NOT_NULL
	 *
	 * @return {@code this}, for method chaining
	 */
	public InFragment addValue(Object value) {
		values.add( value );
		return this;
	}

	public InFragment addValues(Object[] values) {
		Collections.addAll( this.values, values );
		return this;
	}

	public InFragment setColumn(String columnName) {
		this.columnName = columnName;
		return this;
	}

	public InFragment setColumn(String alias, String columnName) {
		this.columnName = StringHelper.qualify( alias, columnName );
		return setColumn( this.columnName );
	}

	public InFragment setFormula(String alias, String formulaTemplate) {
		this.columnName = StringHelper.replace( formulaTemplate, Template.TEMPLATE, alias );
		return setColumn( this.columnName );
	}

	public List<Object> getValues() {
		return values;
	}

	public String toFragmentString() {
		final StringBuilder buf = new StringBuilder( values.size() * 5 );

		switch ( values.size() ) {
			case 0: {
				return "0=1";
			}
			case 1: {
				Object value = values.get( 0 );
				buf.append( columnName );

				if ( NULL.equals( value ) ) {
					buf.append( " is null" );
				}
				else {
					if ( NOT_NULL.equals( value ) ) {
						buf.append( " is not null" );
					}
					else {
						buf.append( '=' ).append( value );
					}
				}
				return buf.toString();
			}
			default: {
				boolean allowNull = false;

				for ( Object value : values ) {
					if ( NULL.equals( value ) ) {
						allowNull = true;
					}
					else {
						if ( NOT_NULL.equals( value ) ) {
							throw new IllegalArgumentException( "not null makes no sense for in expression" );
						}
					}
				}

				if ( allowNull ) {
					buf.append( '(' )
							.append( columnName )
							.append( " is null or " )
							.append( columnName )
							.append( " in (" );
				}
				else {
					buf.append( columnName ).append( " in (" );
				}

				for ( Object value : values ) {
					if ( !NULL.equals( value ) ) {
						buf.append( value );
						buf.append( ", " );
					}
				}

				buf.setLength( buf.length() - 2 );

				if ( allowNull ) {
					buf.append( "))" );
				}
				else {
					buf.append( ')' );
				}

				return buf.toString();
			}
		}
	}
}
