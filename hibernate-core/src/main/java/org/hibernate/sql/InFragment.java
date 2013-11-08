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
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.internal.util.StringHelper;

/**
 * An SQL IN expression.
 * <br>
 * <code>... in(...)</code>
 * <br>
 *
 * @author Gavin King
 */
public class InFragment {

	public static final String NULL = "null";
	public static final String NOT_NULL = "not null";

	private String columnName;
	private List<Object> values = new ArrayList<Object>();

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

	public String toFragmentString() {
		if ( values.size() == 0 ) {
			return "1=2";
		}

		StringBuilder buf = new StringBuilder( values.size() * 5 );

		if ( values.size() == 1 ) {
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
			buf.append( '(' ).append( columnName ).append( " is null or " ).append( columnName ).append( " in (" );
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
