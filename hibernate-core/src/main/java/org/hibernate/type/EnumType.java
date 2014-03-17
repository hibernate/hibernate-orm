/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;

import org.jboss.logging.Logger;

/**
 * Value type mapper for enumerations.
 *
 * Generally speaking, the proper configuration is picked up from the annotations associated with the mapped attribute.
 *
 * There are a few configuration parameters understood by this type mapper:<ul>
 *     <li>
 *         <strong>enumClass</strong> - Names the enumeration class.
 *     </li>
 *     <li>
 *         <strong>useNamed</strong> - Should enum be mapped via name.  Default is to map as ordinal.  Used when
 *         annotations are not used (otherwise {@link javax.persistence.EnumType} is used).
 *     </li>
 *     <li>
 *         <strong>type</strong> - Identifies the JDBC type (via type code) to be used for the column.
 *     </li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class EnumType implements EnhancedUserType, DynamicParameterizedType, LoggableUserType, Serializable {
    private static final Logger LOG = Logger.getLogger( EnumType.class.getName() );

	/**
	 * Keys the enum Class in the config settings
	 */
	public static final String ENUM = "enumClass";

	/**
	 * Keys the boolean decision of whether named value mapping should be
	 * used, as opposed to ordinal.
	 */
	public static final String NAMED = "useNamed";

	/**
	 * Keys the JDBC type code to be used to represent the enum values.
	 * Typically this is either {@link java.sql.Types#INTEGER} (ORDINAL) or
	 * {@link java.sql.Types#VARCHAR} (NAMED)
	 */
	public static final String TYPE = "type";

	private Class<? extends Enum> enumClass;
	private EnumValueMapper enumValueMapper;
	private int sqlType = Types.INTEGER;  // before any guessing

	@Override
	public int[] sqlTypes() {
		return new int[] { sqlType };
	}

	@Override
	public Class<? extends Enum> returnedClass() {
		return enumClass;
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		return x == y;
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		return x == null ? 0 : x.hashCode();
	}

	@Override
	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws SQLException {
		if ( enumValueMapper == null ) {
			resolveEnumValueMapper( rs,  names[0] );
		}
		return enumValueMapper.getValue( rs, names );
	}

	private void resolveEnumValueMapper(ResultSet rs, String name) {
		if ( enumValueMapper == null ) {
			try {
				resolveEnumValueMapper( rs.getMetaData().getColumnType( rs.findColumn( name ) ) );
			}
			catch (Exception e) {
				// because some drivers do not implement this
				LOG.debugf(
						"JDBC driver threw exception calling java.sql.ResultSetMetaData.getColumnType; " +
								"using fallback determination [%s] : %s",
						enumClass.getName(),
						e.getMessage()
				);
				// peek at the result value to guess type (this is legacy behavior)
				try {
					Object value = rs.getObject( name );
					if ( Number.class.isInstance( value ) ) {
						treatAsOrdinal();
					}
					else {
						treatAsNamed();
					}
				}
				catch (SQLException ignore) {
					treatAsOrdinal();
				}
			}
		}
	}

	private void resolveEnumValueMapper(int columnType) {
		// fallback for cases where not enough parameter/parameterization information was passed in
		if ( isOrdinal( columnType ) ) {
			treatAsOrdinal();
		}
		else {
			treatAsNamed();
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if ( enumValueMapper == null ) {
			resolveEnumValueMapper( st, index );
		}
		enumValueMapper.setValue( st, (Enum) value, index );
	}

	private void resolveEnumValueMapper(PreparedStatement st, int index) {
		if ( enumValueMapper == null ) {
			try {
				resolveEnumValueMapper( st.getParameterMetaData().getParameterType( index ) );
			}
			catch (Exception e) {
				// because some drivers do not implement this
				LOG.debugf(
						"JDBC driver threw exception calling java.sql.ParameterMetaData#getParameterType; " +
								"falling back to ordinal-based enum mapping [%s] : %s",
						enumClass.getName(),
						e.getMessage()
				);
				// Originally, this was simply treatAsOrdinal().  But, for DBs that do not implement the above, enums
				// were treated as ordinal even when the *.hbm.xml explicitly define the type sqlCode.  By default,
				// this is essentially the same anyway, since sqlType is defaulted to Integer.
				resolveEnumValueMapper( sqlType );
			}
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
	public Serializable disassemble(Object value) throws HibernateException {
		return ( Serializable ) value;
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		final ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );

		// IMPL NOTE : be protective about not setting enumValueMapper (i.e. calling treatAsNamed/treatAsOrdinal)
		// in cases where we do not have enough information.  In such cases we do additional checks
		// as part of nullSafeGet/nullSafeSet to query against the JDBC metadata to make the determination.

		if ( reader != null ) {
			enumClass = reader.getReturnedClass().asSubclass( Enum.class );

			final boolean isOrdinal;
			final javax.persistence.EnumType enumType = getEnumType( reader );
			if ( enumType == null ) {
				isOrdinal = true;
			}
			else if ( javax.persistence.EnumType.ORDINAL.equals( enumType ) ) {
				isOrdinal = true;
			}
			else if ( javax.persistence.EnumType.STRING.equals( enumType ) ) {
				isOrdinal = false;
			}
			else {
				throw new AssertionFailure( "Unknown EnumType: " + enumType );
			}

			if ( isOrdinal ) {
				treatAsOrdinal();
			}
			else {
				treatAsNamed();
			}
			sqlType = enumValueMapper.getSqlType();
		}
		else {
			String enumClassName = (String) parameters.get( ENUM );
			if( StringHelper.isEmpty( enumClassName )){
				enumClassName = (String)parameters.get( DynamicParameterizedType.RETURNED_CLASS );
			}
			try {
				enumClass = ReflectHelper.classForName( enumClassName, this.getClass() ).asSubclass( Enum.class );
			}
			catch ( ClassNotFoundException exception ) {
				throw new HibernateException( "Enum class not found", exception );
			}

			final Object useNamedSetting = parameters.get( NAMED );
			if ( useNamedSetting != null ) {
				final boolean useNamed = ConfigurationHelper.getBoolean( NAMED, parameters );
				if ( useNamed ) {
					treatAsNamed();
				}
				else {
					treatAsOrdinal();
				}
				sqlType = enumValueMapper.getSqlType();
			}
		}

		final String type = (String) parameters.get( TYPE );
		if ( type != null ) {
			sqlType = Integer.decode( type );
		}
	}

	private void treatAsOrdinal() {
		if ( enumValueMapper == null || ! OrdinalEnumValueMapper.class.isInstance( enumValueMapper ) ) {
			enumValueMapper = new OrdinalEnumValueMapper();
			sqlType = enumValueMapper.getSqlType();
		}
	}

	private void treatAsNamed() {
		if ( enumValueMapper == null || ! NamedEnumValueMapper.class.isInstance( enumValueMapper ) ) {
			enumValueMapper = new NamedEnumValueMapper();
			sqlType = enumValueMapper.getSqlType();
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

	@Override
	public String objectToSQLString(Object value) {
		return enumValueMapper.objectToSQLString( (Enum) value );
	}

	@Override
	public String toXMLString(Object value) {
		return enumValueMapper.toXMLString( (Enum) value );
	}

	@Override
	public Object fromXMLString(String xmlValue) {
		return enumValueMapper.fromXMLString( xmlValue );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( enumValueMapper != null ) {
			return enumValueMapper.toXMLString( (Enum) value );
		}
		return value.toString();
	}

	private static interface EnumValueMapper extends Serializable {
		public int getSqlType();
		public Enum getValue(ResultSet rs, String[] names) throws SQLException;
		public void setValue(PreparedStatement st, Enum value, int index) throws SQLException;

		public String objectToSQLString(Enum value);
		public String toXMLString(Enum value);
		public Enum fromXMLString(String xml);
	}

	public abstract class EnumValueMapperSupport implements EnumValueMapper {
		protected abstract Object extractJdbcValue(Enum value);

		@Override
		public void setValue(PreparedStatement st, Enum value, int index) throws SQLException {
			final Object jdbcValue = value == null ? null : extractJdbcValue( value );

			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( jdbcValue == null ) {
				if ( traceEnabled ) {
					LOG.trace(String.format("Binding null to parameter: [%s]", index));
				}
				st.setNull( index, getSqlType() );
				return;
			}

			if ( traceEnabled ) {
				LOG.trace(String.format("Binding [%s] to parameter: [%s]", jdbcValue, index));
			}
			st.setObject( index, jdbcValue, EnumType.this.sqlType );
		}
	}

	private class OrdinalEnumValueMapper extends EnumValueMapperSupport implements EnumValueMapper, Serializable {
		private transient Enum[] enumsByOrdinal;

		@Override
		public int getSqlType() {
			return Types.INTEGER;
		}

		@Override
		public Enum getValue(ResultSet rs, String[] names) throws SQLException {
			final int ordinal = rs.getInt( names[0] );
			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( rs.wasNull() ) {
				if ( traceEnabled ) {
					LOG.trace(String.format("Returning null as column [%s]", names[0]));
				}
				return null;
			}

			final Enum enumValue = fromOrdinal( ordinal );
			if ( traceEnabled ) {
				LOG.trace(String.format("Returning [%s] as column [%s]", enumValue, names[0]));
			}
			return enumValue;
		}

		private Enum fromOrdinal(int ordinal) {
			final Enum[] enumsByOrdinal = enumsByOrdinal();
			if ( ordinal < 0 || ordinal >= enumsByOrdinal.length ) {
				throw new IllegalArgumentException(
						String.format(
								"Unknown ordinal value [%s] for enum class [%s]",
								ordinal,
								enumClass.getName()
						)
				);
			}
			return enumsByOrdinal[ordinal];

		}

		private Enum[] enumsByOrdinal() {
			if ( enumsByOrdinal == null ) {
				enumsByOrdinal = enumClass.getEnumConstants();
				if ( enumsByOrdinal == null ) {
					throw new HibernateException( "Failed to init enum values" );
				}
			}
			return enumsByOrdinal;
		}

		@Override
		public String objectToSQLString(Enum value) {
			return toXMLString( value );
		}

		@Override
		public String toXMLString(Enum value) {
			return Integer.toString( value.ordinal() );
		}

		@Override
		public Enum fromXMLString(String xml) {
			return fromOrdinal( Integer.parseInt( xml ) );
		}

		@Override
		protected Object extractJdbcValue(Enum value) {
			return value.ordinal();
		}
	}

	private class NamedEnumValueMapper extends EnumValueMapperSupport implements EnumValueMapper, Serializable {
		@Override
		public int getSqlType() {
			return Types.VARCHAR;
		}

		@Override
		public Enum getValue(ResultSet rs, String[] names) throws SQLException {
			final String value = rs.getString( names[0] );

			final boolean traceEnabled = LOG.isTraceEnabled();
			if ( rs.wasNull() ) {
				if ( traceEnabled ) {
					LOG.trace(String.format("Returning null as column [%s]", names[0]));
				}
				return null;
			}

			final Enum enumValue = fromName( value );
			if ( traceEnabled ) {
				LOG.trace(String.format("Returning [%s] as column [%s]", enumValue, names[0]));
			}
			return enumValue;
		}

		private Enum fromName(String name) {
			try {
			    if(name == null) {
			        return null;
			    }
				return Enum.valueOf( enumClass, name.trim() );
			}
			catch ( IllegalArgumentException iae ) {
				throw new IllegalArgumentException(
						String.format(
								"Unknown name value [%s] for enum class [%s]",
								name,
								enumClass.getName()
						)
				);
			}
		}

		@Override
		public String objectToSQLString(Enum value) {
			return '\'' + toXMLString( value ) + '\'';
		}

		@Override
		public String toXMLString(Enum value) {
			return value.name();
		}

		@Override
		public Enum fromXMLString(String xml) {
			return fromName( xml );
		}

		@Override
		protected Object extractJdbcValue(Enum value) {
			return value.name();
		}
	}

	public boolean isOrdinal() {
		return isOrdinal( sqlType );
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
}
