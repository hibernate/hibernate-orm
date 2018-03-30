/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.compositeusertype.nested;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

public class PointType implements CompositeUserType {
	public static final CompositeCustomType TYPE = new CompositeCustomType( new PointType() );

	private static final String[] PROPERTY_NAMES = { "x", "y" };
	private static final Type[] PROPERTY_TYPES = { IntegerType.INSTANCE, IntegerType.INSTANCE };

	@Override
	public String[] getPropertyNames() {
		return PROPERTY_NAMES;
	}

	@Override
	public Type[] getPropertyTypes() {
		return PROPERTY_TYPES;
	}

	@Override
	public Object getPropertyValue(Object component, int property) throws HibernateException {
		switch ( property ) {
			case 0:
				return ( (Point) component ).getX();
			case 1:
				return ( (Point) component ).getY();
			default:
				throw new IndexOutOfBoundsException( "Invalid property index: " + property );
		}
	}

	@Override
	public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
		throw new UnsupportedOperationException( "Immutable type" );
	}

	@Override
	public Class returnedClass() {
		return Point.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return Objects.hashCode( x );
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		int x = rs.getInt( names[0] );
		boolean xNull = rs.wasNull();
		int y = rs.getInt( names[1] );
		boolean yNull = rs.wasNull();
		return xNull || yNull ? null : new Point( x, y );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, Types.INTEGER );
			st.setNull( index + 1, Types.INTEGER );
		}
		else {
			Point p = (Point) value;
			st.setInt( index, p.getX() );
			st.setInt( index + 1, p.getY() );
		}
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
		return (Point) value;
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return original;
	}
}
