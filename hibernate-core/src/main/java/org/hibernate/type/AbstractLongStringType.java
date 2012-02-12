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
package org.hibernate.type;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;

/**
 * An abstract type for mapping long string SQL types to a Java String.
 * @author Gavin King, Bertrand Renuart (from TextType)
 *
 * @deprecated Use the {@link AbstractStandardBasicType} approach instead
 */
public abstract class AbstractLongStringType extends ImmutableType {

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		String str = (String) value;
		st.setCharacterStream( index, new StringReader(str), str.length() );
	}

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {

			// Retrieve the value of the designated column in the current row of this
			// ResultSet object as a java.io.Reader object
			Reader charReader = rs.getCharacterStream(name);

			// if the corresponding SQL value is NULL, the reader we got is NULL as well
			if (charReader==null) return null;

			// Fetch Reader content up to the end - and put characters in a StringBuilder
			StringBuilder sb = new StringBuilder();
			try {
				char[] buffer = new char[2048];
				while (true) {
					int amountRead = charReader.read(buffer, 0, buffer.length);
					if ( amountRead == -1 ) break;
					sb.append(buffer, 0, amountRead);
				}
			}
			catch (IOException ioe) {
				throw new HibernateException( "IOException occurred reading text", ioe );
			}
			finally {
				try {
					charReader.close();
				}
				catch (IOException e) {
					throw new HibernateException( "IOException occurred closing stream", e );
				}
			}

			// Return StringBuilder content as a large String
			return sb.toString();
	}

	public Class getReturnedClass() {
		return String.class;
	}

	public String toString(Object val) {
		return (String) val;
	}
	public Object fromStringValue(String xml) {
		return xml;
	}

}