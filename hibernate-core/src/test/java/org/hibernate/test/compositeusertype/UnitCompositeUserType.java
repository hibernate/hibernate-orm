/**
 * 
 */
package org.hibernate.test.compositeusertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class UnitCompositeUserType implements CompositeUserType {

	@Override
	public String[] getPropertyNames() {
		return new String[] { "unit", "currency_name" };
	}

	@Override
	public Type[] getPropertyTypes() {
		return new Type[] { StringType.INSTANCE, StringType.INSTANCE };
	}

	@Override
	public Object getPropertyValue(Object component, int property) throws HibernateException {
		Object result = null;
		if ( component == Percent.INSTANCE ) {
			if ( property == 0 ) {
				result = "PERCENT";
			}
		}
		else if ( component instanceof Currency ) {
			if ( property == 0 ) {
				result = "CURRENCY";
			}
			else if ( property == 1 ) {
				Currency curr = (Currency) component;
				result = curr.getName();
			}
		}
		return result;
	}

	@Override
	public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
		throw new UnsupportedOperationException( "Units are not mutable" );
	}

	@Override
	public Class<?> returnedClass() {
		return Unit.class;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return ( x == y ) || ( x != null && x.equals( y ) );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x == null ? 0 : x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
		Object result = null;
		String unitType = rs.getString( names[0] );
		String currencyName = rs.getString( names[1] );
		if ( "PERCENT".equals( unitType ) ) {
			result = Percent.INSTANCE;
		}
		else if ( "CURRENCY".equals( unitType ) ) {
			result = new Currency( currencyName );
		}
		return result;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			st.setNull( index, Types.VARCHAR );
			st.setNull( index + 1, Types.VARCHAR );
		}
		else if ( value == Percent.INSTANCE ) {
			st.setString( index, "PERCENT" );
			st.setNull( index + 1, Types.VARCHAR );
		}
		else if ( value instanceof Currency ) {
			st.setString( index, "CURRENCY" );
			st.setString( index + 1, ( (Currency) value ).getName() );
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
		final Serializable[] result = new Serializable[2];
		if ( value == Percent.INSTANCE ) {
			result[0] = "PERCENT";
		}
		else if ( value instanceof Currency ) {
			result[0] = "CURRENCY";
			result[1] = ( (Currency) value ).getName();
		}
		return result;
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		Object result = null;
		Serializable[] fields = (Serializable[]) cached;
		if ( "PERCENT".equals( fields[0] ) ) {
			result = Percent.INSTANCE;
		}
		else if ( "CURRENCY".equals( fields[0] ) ) {
			result = new Currency( (String) fields[1] );
		}
		return result;
	}

	@Override
	public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return original;
	}

}
