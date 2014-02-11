package org.hibernate.test.collection.set;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

public class PersonInfoType implements UserType {

	@Override
	public int[] sqlTypes() {
		return new int[] { IntegerType.INSTANCE.sqlType(), StringType.INSTANCE.sqlType() };
	}

	@Override
	public Class<?> returnedClass() {
		return PersonInfo.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return x == null ? y == null : x.equals( y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x == null ? 0 : x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		PersonInfo result = null;
		final int height = rs.getInt( names[0] );
		if ( height > 0 ) {
			final String hairColor = rs.getString( names[1] );
			result = new PersonInfo( height, hairColor );
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, IntegerType.INSTANCE.sqlType() );
			st.setNull( index + 1, StringType.INSTANCE.sqlType() );
		}
		else {
			final PersonInfo pi = (PersonInfo) value;
			st.setInt( index, pi.getHeight() );
			if ( pi.getHairColor() == null ) {
				st.setNull( index + 1, StringType.INSTANCE.sqlType() );
			}
			else {
				st.setString( index + 1, pi.getHairColor() );
			}
		}
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		PersonInfo result = null;
		if ( value != null ) {
			final PersonInfo personInfo = (PersonInfo) value;
			result = new PersonInfo( personInfo.getHeight(), personInfo.getHairColor() );
		}
		return result;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		Serializable[] result = null;
		if ( value != null ) {
			final PersonInfo pi = (PersonInfo) value;
			result = new Serializable[] { pi.getHeight(), pi.getHairColor() };
		}
		return result;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		PersonInfo result = null;
		if ( cached != null ) {
			final Serializable[] serializable = (Serializable[]) cached;
			result = new PersonInfo( (Integer) serializable[0], (String) serializable[1] );
		}
		return result;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		final PersonInfo piTarget = (PersonInfo) target;
		final PersonInfo piOriginal = (PersonInfo) original;
		piTarget.setHeight( piOriginal.getHeight() );
		piTarget.setHairColor( piOriginal.getHairColor() );
		return piTarget;
	}

}
