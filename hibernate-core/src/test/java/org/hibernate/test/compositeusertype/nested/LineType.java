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
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

public class LineType implements CompositeUserType {

	private static final String[] PROPERTY_NAMES = { "p1", "p2" };
	private static final Type[] PROPERTY_TYPES = { PointType.TYPE, PointType.TYPE };

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
				return ( (Line) component ).getP1();
			case 1:
				return ( (Line) component ).getP2();
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
		return Line.class;
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
		Point p1 = (Point) PointType.TYPE.nullSafeGet( rs, Arrays.copyOfRange( names, 0, 2 ), session, owner );
		Point p2 = (Point) PointType.TYPE.nullSafeGet( rs, Arrays.copyOfRange( names, 2, 4 ), session, owner );
		return p1 == null || p2 == null ? null : new Line( p1, p2 );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( value == null ) {
			PointType.TYPE.nullSafeSet( st, null, index, session );
			PointType.TYPE.nullSafeSet( st, null, index + 2, session );
		}
		else {
			Line l = (Line) value;
			PointType.TYPE.nullSafeSet( st, l.getP1(), index, session );
			PointType.TYPE.nullSafeSet( st, l.getP2(), index + 2, session );
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
		return (Line) value;
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
