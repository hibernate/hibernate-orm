/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Emmanuel Bernard
 */
public class MyOidType implements CompositeUserType {

	public static final String[] PROPERTY_NAMES = new String[]{
			"high", "middle", "low", "other"
	};
	public static final Type[] TYPES = new Type[]{
			StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER, StandardBasicTypes.INTEGER
	};

	@Override
	public String[] getPropertyNames() {
		return PROPERTY_NAMES;
	}

	@Override
	public Type[] getPropertyTypes() {
		return TYPES;
	}

	@Override
	public Object getPropertyValue(Object aObject, int i) throws HibernateException {
		MyOid dbOid = (MyOid) aObject;
		switch ( i ) {
			case 0:
				return dbOid.getHigh();
			case 1:
				return dbOid.getMiddle();
			case 2:
				return dbOid.getLow();
			case 3:
				return dbOid.getOther();
			default:
				throw new HibernateException( "Unsupported property index " + i );
		}

	}

	@Override
	public void setPropertyValue(Object aObject, int i, Object aObject1) throws HibernateException {
		MyOid dbOid = (MyOid) aObject;
		switch ( i ) {
			case 0:
				dbOid.setHigh( (Integer) aObject1 );
			case 1:
				dbOid.setMiddle( (Integer) aObject1 );
			case 2:
				dbOid.setLow( (Integer) aObject1 );
			case 3:
				dbOid.setOther( (Integer) aObject1 );
			default:
				throw new HibernateException( "Unsupported property index " + i );
		}
	}

	@Override
	public Class returnedClass() {
		return MyOid.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		if ( x == y ) return true;
		if ( x == null || y == null ) return false;

		MyOid oid1 = (MyOid) x;
		MyOid oid2 = (MyOid) y;

		if ( oid1.getHigh() != oid2.getHigh() ) {
			return false;
		}
		if ( oid1.getMiddle() != oid2.getMiddle() ) {
			return false;
		}
		if ( oid1.getLow() != oid2.getLow() ) {
			return false;
		}
		return oid1.getOther() == oid2.getOther();

	}

	@Override
	public int hashCode(Object aObject) throws HibernateException {
		return aObject.hashCode();
	}

	@Override
	public Object nullSafeGet(
			ResultSet aResultSet, String[] names, SharedSessionContractImplementor session, Object aObject
	) throws HibernateException, SQLException {
		Integer highval = StandardBasicTypes.INTEGER.nullSafeGet( aResultSet, names[0], session );
		Integer midval = StandardBasicTypes.INTEGER.nullSafeGet( aResultSet, names[1], session );
		Integer lowval = StandardBasicTypes.INTEGER.nullSafeGet( aResultSet, names[2], session );
		Integer other = StandardBasicTypes.INTEGER.nullSafeGet( aResultSet, names[3], session );

		return new MyOid( highval, midval, lowval, other );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement aPreparedStatement, Object value, int index, SharedSessionContractImplementor session
	) throws HibernateException, SQLException {
		MyOid c;
		if ( value == null ) {
			// todo is this correct?
			throw new HibernateException( "Oid object may not be null" );
		}
		else {
			c = (MyOid) value;
		}

		StandardBasicTypes.INTEGER.nullSafeSet( aPreparedStatement, c.getHigh(), index, session );
		StandardBasicTypes.INTEGER.nullSafeSet( aPreparedStatement, c.getMiddle(), index + 1, session );
		StandardBasicTypes.INTEGER.nullSafeSet( aPreparedStatement, c.getLow(), index + 2, session );
		StandardBasicTypes.INTEGER.nullSafeSet( aPreparedStatement, c.getOther(), index + 3, session );
	}

	@Override
	public Object deepCopy(Object aObject) throws HibernateException {
		MyOid oldOid = (MyOid) aObject;

		return new MyOid( oldOid.getHigh(), oldOid.getMiddle(), oldOid.getLow(), oldOid.getOther() );
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
		return (Serializable) deepCopy( value );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object aObject)
			throws HibernateException {
		return deepCopy( cached );
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object aObject2)
			throws HibernateException {
		// we are immutable. return original
		return original;
	}

}
