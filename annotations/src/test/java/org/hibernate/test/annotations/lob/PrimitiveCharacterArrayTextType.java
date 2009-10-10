//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.test.annotations.lob;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.type.TextType;

/**
 * A type that maps an SQL LONGVARCHAR to a Java char [].
 * 
 * @author Strong Liu
 */
public class PrimitiveCharacterArrayTextType extends TextType {
	public Class getReturnedClass() {
		return char[].class;
	}

	@Override
	public Object get(ResultSet rs, String name) throws HibernateException,
			SQLException {
		String text = (String) super.get(rs, name);
		if (text == null)
			return null;
		return text.toCharArray();
	}

	@Override
	public void set(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException {
		char[] cs = (char[]) value;
		String text = String.valueOf(cs);

		super.set(st, text, index);
	}

	@Override
	public String toString(Object val) {
		return String.valueOf(val);
	}

}
