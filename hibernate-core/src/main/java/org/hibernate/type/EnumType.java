/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.Properties;
import javax.persistence.Enumerated;
import javax.persistence.MapKeyEnumerated;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.ReflectHelper;
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
public class EnumType implements EnhancedUserType, DynamicParameterizedType,LoggableUserType, Serializable {
	private static final Logger LOG = CoreLogging.logger( EnumType.class );

	public static final String ENUM = "enumClass";
	public static final String NAMED = "useNamed";
	public static final String TYPE = "type";

	private Class<? extends Enum> enumClass;
	private EnumValueMapper enumValueMapper;
	private int sqlType;

	@Override
	public void setParameterValues(Properties parameters) {
		// IMPL NOTE: we handle 2 distinct cases here:
		// 		1) we are passed a ParameterType instance in the incoming Properties - generally
		//			speaking this indicates the annotation-binding case, and the passed ParameterType
		//			represents information about the attribute and annotation
		//		2) we are not passed a ParameterType - generally this indicates a hbm.xml binding case.
		final ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );

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
				this.enumValueMapper = new OrdinalEnumValueMapper();
			}
			else {
				this.enumValueMapper = new NamedEnumValueMapper();
			}
			sqlType = enumValueMapper.getSqlType();
		}
		else {
			final String enumClassName = (String) parameters.get( ENUM );
			try {
				enumClass = ReflectHelper.classForName( enumClassName, this.getClass() ).asSubclass( Enum.class );
			}
			catch ( ClassNotFoundException exception ) {
				throw new HibernateException( "Enum class not found: " + enumClassName, exception );
			}

			this.enumValueMapper = interpretParameters( parameters );
			this.sqlType = enumValueMapper.getSqlType();
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

	private EnumValueMapper interpretParameters(Properties parameters) {
		if ( parameters.containsKey( NAMED ) ) {
			final boolean useNamed = ConfigurationHelper.getBoolean( NAMED, parameters );
			if ( useNamed ) {
				return new NamedEnumValueMapper();
			}
			else {
				return new OrdinalEnumValueMapper();
			}
		}

		if ( parameters.containsKey( TYPE ) ) {
			final int type = Integer.decode( (String) parameters.get( TYPE ) );
			if ( isNumericType( type ) ) {
				return new OrdinalEnumValueMapper();
			}
			else if ( isCharacterType( type ) ) {
				return new OrdinalEnumValueMapper();
			}
			else {
				throw new HibernateException(
						String.format(
								Locale.ENGLISH,
								"Passed JDBC type code [%s] not recognized as numeric nor character",
								type
						)
				);
			}
		}

		// the fallback
		return new OrdinalEnumValueMapper();
	}

	private boolean isCharacterType(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case Types.CHAR:
			case Types.LONGVARCHAR:
			case Types.VARCHAR: {
				return true;
			}
			default: {
				return false;
			}
		}
	}

	private boolean isNumericType(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case Types.INTEGER:
			case Types.NUMERIC:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.BIGINT:
			case Types.DECIMAL:
			case Types.DOUBLE:
			case Types.FLOAT: {
				return true;
			}
			default:
				return false;
		}
	}

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
			throw new AssertionFailure( "EnumType (" + enumClass.getName() + ") not properly, fully configured" );
		}
		return enumValueMapper.getValue( rs, names );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
		if ( enumValueMapper == null ) {
			throw new AssertionFailure( "EnumType (" + enumClass.getName() + ") not properly, fully configured" );
		}
		enumValueMapper.setValue( st, (Enum) value, index );
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

	public boolean isOrdinal() {
		return enumValueMapper instanceof OrdinalEnumValueMapper;
	}

	private interface EnumValueMapper extends Serializable {
		int getSqlType();
		Enum getValue(ResultSet rs, String[] names) throws SQLException;
		void setValue(PreparedStatement st, Enum value, int index) throws SQLException;

		String objectToSQLString(Enum value);
		String toXMLString(Enum value);
		Enum fromXMLString(String xml);
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
				if (name == null) {
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

}
