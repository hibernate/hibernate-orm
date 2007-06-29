//$Id: AdaptedImmutableType.java 7238 2005-06-20 09:17:00Z oneovthafew $
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
