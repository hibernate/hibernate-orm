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
 */
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.util.StringHelper;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enum type mapper
 * Try and find the appropriate SQL type depending on column metadata
 * <p/>
 * TODO implements readobject/writeobject to recalculate the enumclasses
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class EnumType implements EnhancedUserType, ParameterizedType, Serializable {
	/**
	 * This is the old scheme where logging of parameter bindings and value extractions
	 * was controlled by the trace level enablement on the 'org.hibernate.type' package...
	 * <p/>
	 * Originally was cached such because of performance of looking up the logger each time
	 * in order to check the trace-enablement.  Driving this via a central Log-specific class
	 * would alleviate that performance hit, and yet still allow more "normal" logging usage/config.
	 */
	private static final boolean IS_VALUE_TRACING_ENABLED = LoggerFactory.getLogger( StringHelper.qualifier( Type.class.getName() ) ).isTraceEnabled();
	private transient Logger log;

	private Logger log() {
		if ( log == null ) {
			log = LoggerFactory.getLogger( getClass() );
		}
		return log;
	}

	public static final String ENUM = "enumClass";
	public static final String SCHEMA = "schema";
	public static final String CATALOG = "catalog";
	public static final String TABLE = "table";
	public static final String COLUMN = "column";
	public static final String TYPE = "type";

	private Class<? extends Enum> enumClass;
	private transient Object[] enumValues;
	private String catalog;
	private String schema;
	private int sqlType = Types.INTEGER; //before any guessing

	public int[] sqlTypes() {
		return new int[]{sqlType};
	}

	public Class<? extends Enum> returnedClass() {
		return enumClass;
	}

	public boolean equals(Object x, Object y) throws HibernateException {
		return x == y;
	}

	public int hashCode(Object x) throws HibernateException {
		return x == null ? 0 : x.hashCode();
	}

	
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException, SQLException {
		Object object = rs.getObject( names[0] );
		if ( rs.wasNull() ) {
			if ( IS_VALUE_TRACING_ENABLED ) {
				log().debug( "Returning null as column {}", names[0] );
			}
			return null;
		}
		if ( object instanceof Number ) {
			initEnumValues();
			int ordinal = ( (Number) object ).intValue();
			if ( ordinal < 0 || ordinal >= enumValues.length ) {
				throw new IllegalArgumentException( "Unknown ordinal value for enum " + enumClass + ": " + ordinal );
			}
			if ( IS_VALUE_TRACING_ENABLED ) {
				log().debug( "Returning '{}' as column {}", ordinal, names[0] );
			}
			return enumValues[ordinal];
		}
		else {
			String name = (String) object;
			if ( IS_VALUE_TRACING_ENABLED ) {
				log().debug( "Returning '{}' as column {}", name, names[0] );
			}
			try {
				return Enum.valueOf( enumClass, name );
			}
			catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException( "Unknown name value for enum " + enumClass + ": " + name, iae );
			}
		}
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		//if (!guessed) guessType( st, index );
		if ( value == null ) {
			if ( IS_VALUE_TRACING_ENABLED ) log().debug( "Binding null to parameter: {}", index );
			st.setNull( index, sqlType );
		}
		else {
			boolean isOrdinal = isOrdinal( sqlType );
			if ( isOrdinal ) {
				int ordinal = ( (Enum<?>) value ).ordinal();
				if ( IS_VALUE_TRACING_ENABLED ) {
					log().debug( "Binding '{}' to parameter: {}", ordinal, index );
				}
				st.setObject( index, Integer.valueOf( ordinal ), sqlType );
			}
			else {
				String enumString = ( (Enum<?>) value ).name();
				if ( IS_VALUE_TRACING_ENABLED ) {
					log().debug( "Binding '{}' to parameter: {}", enumString, index );
				}
				st.setObject( index, enumString, sqlType );
			}
		}
	}

	private boolean isOrdinal(int paramType) {
		switch ( paramType ) {
			case Types.INTEGER:
			case Types.NUMERIC:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.BIGINT:
			case Types.DECIMAL: //for Oracle Driver
			case Types.DOUBLE:  //for Oracle Driver
			case Types.FLOAT:   //for Oracle Driver
				return true;
			case Types.CHAR:
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
				return false;
			default:
				throw new HibernateException( "Unable to persist an Enum in a column of SQL Type: " + paramType );
		}
	}

	public Object deepCopy(Object value) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(Object value) throws HibernateException {
		return (Serializable) value;
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	public void setParameterValues(Properties parameters) {
		String enumClassName = parameters.getProperty( ENUM );
		try {
			enumClass = ReflectHelper.classForName( enumClassName, this.getClass() ).asSubclass( Enum.class );
		}
		catch (ClassNotFoundException exception) {
			throw new HibernateException( "Enum class not found", exception );
		}
		// is might be good to call it here, to see a possible error immediately
		// initEnumValue();
		
		//nullify unnullified properties yuck!
		schema = parameters.getProperty( SCHEMA );
		if ( "".equals( schema ) ) schema = null;
		catalog = parameters.getProperty( CATALOG );
		if ( "".equals( catalog ) ) catalog = null;
//		table = parameters.getProperty( TABLE );
//		column = parameters.getProperty( COLUMN );
		String type = parameters.getProperty( TYPE );
		if ( type != null ) {
			sqlType = Integer.decode( type ).intValue();
//			guessed = true;
		}
	}

	/**
	 * Lazy init of {@link #enumValues}.
	 */
	private void initEnumValues() {
		if ( enumValues == null ) {
			try {
				Method method = enumClass.getDeclaredMethod( "values" );
				enumValues = (Object[]) method.invoke( null );
			}
			catch (Exception e) {
				throw new HibernateException( "Error while accessing enum.values(): " + enumClass, e );
			}
		}
	}

	// is might be good to call initEnumValues() here, to see a possible error immediatelly, otherwise leave it commented
//	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
//		initEnumValues();
//		ois.defaultReadObject();
//	}

	public String objectToSQLString(Object value) {
		boolean isOrdinal = isOrdinal( sqlType );
		if ( isOrdinal ) {
			int ordinal = ( (Enum) value ).ordinal();
			return Integer.toString( ordinal );
		}
		else {
			return '\'' + ( (Enum) value ).name() + '\'';
		}
	}

	public String toXMLString(Object value) {
		boolean isOrdinal = isOrdinal( sqlType );
		if ( isOrdinal ) {
			int ordinal = ( (Enum) value ).ordinal();
			return Integer.toString( ordinal );
		}
		else {
			return ( (Enum) value ).name();
		}
	}

	public Object fromXMLString(String xmlValue) {
		try {
			int ordinal = Integer.parseInt( xmlValue );
			initEnumValues();
			if ( ordinal < 0 || ordinal >= enumValues.length ) {
				throw new IllegalArgumentException( "Unknown ordinal value for enum " + enumClass + ": " + ordinal );
			}
			return enumValues[ordinal];
		}
		catch(NumberFormatException e) {
			try {
				return Enum.valueOf( enumClass, xmlValue );
			}
			catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException( "Unknown name value for enum " + enumClass + ": " + xmlValue, iae );
			}
		}
	}
}
