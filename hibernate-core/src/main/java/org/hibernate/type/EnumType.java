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
import java.lang.annotation.Annotation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import javax.persistence.Enumerated;
import javax.persistence.MapKeyEnumerated;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.jboss.logging.Logger;

/**
 * Enum type mapper
 * Try and find the appropriate SQL type depending on column metadata
 * <p/>
 * TODO implements readobject/writeobject to recalculate the enumclasses
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class EnumType implements EnhancedUserType, DynamicParameterizedType, Serializable {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, EnumType.class.getName());

	public static final String ENUM = "enumClass";
	public static final String SCHEMA = "schema";
	public static final String CATALOG = "catalog";
	public static final String TABLE = "table";
	public static final String COLUMN = "column";
	public static final String TYPE = "type";

	private Class<? extends Enum> enumClass;
	private transient Object[] enumValues;
	private int sqlType = Types.INTEGER; //before any guessing

	public int[] sqlTypes() {
		return new int[] { sqlType };
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


	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws HibernateException, SQLException {
		Object object = rs.getObject( names[0] );
		if ( rs.wasNull() ) {
			if ( LOG.isTraceEnabled() ) LOG.tracev( "Returning null as column {0}", names[0] );
			return null;
		}
		if ( object instanceof Number ) {
			initEnumValues();
			int ordinal = ( ( Number ) object ).intValue();
            if (ordinal < 0 || ordinal >= enumValues.length) throw new IllegalArgumentException("Unknown ordinal value for enum "
                                                                                                + enumClass + ": " + ordinal);
			if ( LOG.isTraceEnabled() ) LOG.tracev( "Returning '{0}' as column {1}", ordinal, names[0] );
			return enumValues[ordinal];
		}
		else {
			String name = ( String ) object;
			if ( LOG.isTraceEnabled() ) LOG.tracev( "Returning '{0}' as column {1}", name, names[0] );
			try {
				return Enum.valueOf( enumClass, name );
			}
			catch ( IllegalArgumentException iae ) {
				throw new IllegalArgumentException( "Unknown name value for enum " + enumClass + ": " + name, iae );
			}
		}
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if ( value == null ) {
			if ( LOG.isTraceEnabled() ) LOG.tracev( "Binding null to parameter: {0}", index );
			st.setNull( index, sqlType );
		}
		else {
			boolean isOrdinal = isOrdinal( sqlType );
			if ( isOrdinal ) {
				int ordinal = ( ( Enum<?> ) value ).ordinal();
				if ( LOG.isTraceEnabled() ) LOG.tracev( "Binding '{0}' to parameter: '{1}", ordinal, index );
				st.setObject( index, Integer.valueOf( ordinal ), sqlType );
			}
			else {
				String enumString = ( ( Enum<?> ) value ).name();
				if ( LOG.isTraceEnabled() ) LOG.tracev( "Binding '{0}' to parameter: {1}", enumString, index );
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
		return ( Serializable ) value;
	}

	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	public void setParameterValues(Properties parameters) {
		ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );

		if ( reader != null ) {
			enumClass = reader.getReturnedClass().asSubclass( Enum.class );

			javax.persistence.EnumType enumType = getEnumType( reader );
			if ( enumType != null ) {
				if ( javax.persistence.EnumType.ORDINAL.equals( enumType ) ) {
					sqlType = Types.INTEGER;
				}
				else if ( javax.persistence.EnumType.STRING.equals( enumType ) ) {
					sqlType = Types.VARCHAR;
				}
				else {
					throw new AssertionFailure( "Unknown EnumType: " + enumType );
				}
			}
		}
		else {
			String enumClassName = (String) parameters.get( ENUM );
			try {
				enumClass = ReflectHelper.classForName( enumClassName, this.getClass() ).asSubclass( Enum.class );
			}
			catch ( ClassNotFoundException exception ) {
				throw new HibernateException( "Enum class not found", exception );
			}

			String type = (String) parameters.get( TYPE );
			if ( type != null ) {
				sqlType = Integer.decode( type );
			}
		}
	}

	/**
	 * Lazy init of {@link #enumValues}.
	 */
	private void initEnumValues() {
		if ( enumValues == null ) {
			this.enumValues = enumClass.getEnumConstants();
			if ( enumValues == null ) {
				throw new NullPointerException( "Failed to init enumValues" );
			}
		}
	}

	public String objectToSQLString(Object value) {
		boolean isOrdinal = isOrdinal( sqlType );
		if ( isOrdinal ) {
			int ordinal = ( ( Enum ) value ).ordinal();
			return Integer.toString( ordinal );
		}
		else {
			return '\'' + ( ( Enum ) value ).name() + '\'';
		}
	}

	public String toXMLString(Object value) {
		boolean isOrdinal = isOrdinal( sqlType );
		if ( isOrdinal ) {
			int ordinal = ( ( Enum ) value ).ordinal();
			return Integer.toString( ordinal );
		}
		else {
			return ( ( Enum ) value ).name();
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
		catch ( NumberFormatException e ) {
			try {
				return Enum.valueOf( enumClass, xmlValue );
			}
			catch ( IllegalArgumentException iae ) {
				throw new IllegalArgumentException( "Unknown name value for enum " + enumClass + ": " + xmlValue, iae );
			}
		}
	}

	private javax.persistence.EnumType getEnumType(ParameterType reader) {
		javax.persistence.EnumType enumType = null;
		if ( reader.isPrimaryKey() ) {
			MapKeyEnumerated enumAnn = getAnnotation( reader.getAnnotationsMethod(), MapKeyEnumerated.class );
			if ( enumAnn != null ) {
				enumType = enumAnn.value();
			}
		}
		else {
			Enumerated enumAnn = getAnnotation( reader.getAnnotationsMethod(), Enumerated.class );
			if ( enumAnn != null ) {
				enumType = enumAnn.value();
			}
		}
		return enumType;
	}

	private <T extends Annotation> T getAnnotation(Annotation[] annotations, Class<T> anClass) {
		for ( Annotation annotation : annotations ) {
			if ( anClass.isInstance( annotation ) ) {
				return (T) annotation;
			}
		}
		return null;
	}
}
