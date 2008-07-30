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
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.StringTokenizer;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * <tt>locale</tt>: A type that maps an SQL VARCHAR to a Java Locale.
 * @author Gavin King
 */
public class LocaleType extends ImmutableType implements LiteralType {

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		return fromStringValue( (String) Hibernate.STRING.get(rs, name) );
	}

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		Hibernate.STRING.set(st, value.toString(), index);
	}

	public Object fromStringValue(String string) {
		if (string == null) {
			return null;
		}
		else {
			StringTokenizer tokens = new StringTokenizer(string, "_");
			String language = tokens.hasMoreTokens() ? tokens.nextToken() : "";
			String country = tokens.hasMoreTokens() ? tokens.nextToken() : "";
			// Need to account for allowable '_' within the variant
			String variant = "";
			String sep = "";
			while ( tokens.hasMoreTokens() ) {
				variant += sep + tokens.nextToken();
				sep = "_";
			}
			return new Locale(language, country, variant);
		}
	}
	
	public int compare(Object x, Object y, EntityMode entityMode) {
		return x.toString().compareTo( y.toString() );
	}

	public int sqlType() {
		return Hibernate.STRING.sqlType();
	}

	public String toString(Object value) throws HibernateException {
		return value.toString();
	}

	public Class getReturnedClass() {
		return Locale.class;
	}

	public String getName() {
		return "locale";
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {
		return ( (LiteralType) Hibernate.STRING ).objectToSQLString( value.toString(), dialect );
	}

}






