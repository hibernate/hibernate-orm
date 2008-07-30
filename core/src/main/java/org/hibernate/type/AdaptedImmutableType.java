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

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;

/**
 * Optimize a mutable type, if the user promises not to mutable the
 * instances.
 * 
 * @author Gavin King
 */
public class AdaptedImmutableType extends ImmutableType {
	
	private final NullableType mutableType;

	public AdaptedImmutableType(NullableType mutableType) {
		this.mutableType = mutableType;
	}

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		return mutableType.get(rs, name);
	}

	public void set(PreparedStatement st, Object value, int index) throws HibernateException,
			SQLException {
		mutableType.set(st, value, index);
	}

	public int sqlType() {
		return mutableType.sqlType();
	}

	public String toString(Object value) throws HibernateException {
		return mutableType.toString(value);
	}

	public Object fromStringValue(String xml) throws HibernateException {
		return mutableType.fromStringValue(xml);
	}

	public Class getReturnedClass() {
		return mutableType.getReturnedClass();
	}

	public String getName() {
		return "imm_" + mutableType.getName();
	}
	
	public boolean isEqual(Object x, Object y) {
		return mutableType.isEqual(x, y);
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		return mutableType.getHashCode(x, entityMode);
	}
	
	public int compare(Object x, Object y, EntityMode entityMode) {
		return mutableType.compare(x, y, entityMode);
	}
}
