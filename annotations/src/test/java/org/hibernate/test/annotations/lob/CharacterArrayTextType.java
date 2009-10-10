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

/**
 * A type that maps an SQL LONGVARCHAR to a Java Character [].
 * 
 * @author Strong Liu
 */
public class CharacterArrayTextType extends PrimitiveCharacterArrayTextType {
	
	public Class getReturnedClass() {
		return Character[].class;
	}

	@Override
	public Object get(ResultSet rs, String name) throws HibernateException,
			SQLException {
		char[] text = (char[]) super.get(rs, name);
		if (text == null)
			return null;
		return wrapPrimitive(text);
	}

	@Override
	public void set(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException {
		Character[] cs = (Character[]) value;
		super.set(st, unwrapNonPrimitive(cs), index);
	}

	private Character[] wrapPrimitive(char[] bytes) {
		int length = bytes.length;
		Character[] result = new Character[length];
		for (int index = 0; index < length; index++) {
			result[index] = Character.valueOf(bytes[index]);
		}
		return result;
	}

	private char[] unwrapNonPrimitive(Character[] bytes) {
		int length = bytes.length;
		char[] result = new char[length];
		for (int i = 0; i < length; i++) {
			result[i] = bytes[i].charValue();
		}
		return result;
	}

}
