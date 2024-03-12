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
import java.util.Properties;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MapKeyEnumerated;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;

import org.jboss.logging.Logger;

import static jakarta.persistence.EnumType.ORDINAL;
import static jakarta.persistence.EnumType.STRING;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;

/**
 * Value type mapper for enumerations.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 *
 * @deprecated Use the built-in support for enums
 */
@Deprecated(since="6.2", forRemoval=true)
public class EnumType<T extends Enum<T>>
		implements EnhancedUserType<T>, DynamicParameterizedType, LoggableUserType, TypeConfigurationAware, Serializable {
	private static final Logger LOG = CoreLogging.logger( EnumType.class );

	public static final String ENUM = "enumClass";
	public static final String NAMED = "useNamed";
	public static final String TYPE = "type";

	private Class<T> enumClass;

	private boolean isOrdinal;
	private JdbcType jdbcType;
	private EnumJavaType<T> enumJavaType;

	private TypeConfiguration typeConfiguration;

	public EnumType() {
	}

	public Class<T> getEnumClass() {
		return enumClass;
	}

	@Override
	public JdbcType getJdbcType(TypeConfiguration typeConfiguration) {
		return jdbcType;
	}

	/**
	 * <p>
	 * An instance of this class is "configured" by a call to {@link #setParameterValues},
	 * where configuration parameters are given as entries in a {@link Properties} object.
	 * There are two distinct ways an instance may be configured:
	 * <ul>
	 * <li>one for {@code hbm.xml}-based mapping, and
	 * <li>another for annotation-based or {@code orm.xml}-based mapping.
	 * </ul>
	 * <p>
	 * In the case of annotations or {@code orm.xml}, a {@link ParameterType} is passed to
	 * {@link #setParameterValues} under the key {@value #PARAMETER_TYPE}.
	 * <p>
	 * But in the case of {@code hbm.xml}, there are multiple parameters:
	 * <ul>
	 *     <li>
	 *         {@value #ENUM}, the name of the Java enumeration class.
	 *     </li>
	 *     <li>
	 *         {@value #NAMED}, specifies if the enum should be mapped by name.
	 *         Default is to map as ordinal.
	 *     </li>
	 *     <li>
	 * 			{@value #TYPE}, a JDBC type code (legacy alternative to {@value #NAMED}).
	 *     </li>
	 * </ul>
	 */
	@Override
	public void setParameterValues(Properties parameters) {
		// IMPL NOTE: we handle 2 distinct cases here:
		// 		1) we are passed a ParameterType instance in the incoming Properties - generally
		//			speaking this indicates the annotation-binding case, and the passed ParameterType
		//			represents information about the attribute and annotation
		//		2) we are not passed a ParameterType - generally this indicates a hbm.xml binding case.
		final ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );

		if ( parameters.containsKey( ENUM ) ) {
			final String enumClassName = (String) parameters.get( ENUM );
			try {
				enumClass = (Class<T>) ReflectHelper.classForName( enumClassName, this.getClass() ).asSubclass( Enum.class );
			}
			catch ( ClassNotFoundException exception ) {
				throw new HibernateException("Enum class not found: " + enumClassName, exception);
			}
		}
		else if ( reader != null ) {
			enumClass = (Class<T>) reader.getReturnedClass().asSubclass( Enum.class );
		}

		final JavaType<T> descriptor = typeConfiguration.getJavaTypeRegistry().getDescriptor( enumClass );
		enumJavaType = (EnumJavaType<T>) descriptor;

		if ( parameters.containsKey( TYPE ) ) {
			int jdbcTypeCode = Integer.parseInt( (String) parameters.get( TYPE ) );
			jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( jdbcTypeCode );
			isOrdinal = jdbcType.isInteger()
					// Both, ENUM and NAMED_ENUM are treated like ordinal with respect to the ordering
					|| jdbcType.getDefaultSqlTypeCode() == SqlTypes.ENUM
					|| jdbcType.getDefaultSqlTypeCode() == SqlTypes.NAMED_ENUM;
		}
		else {
			final LocalJdbcTypeIndicators indicators;
			final Long columnLength = reader == null ? null : reader.getColumnLengths()[0];
			if ( parameters.containsKey (NAMED ) ) {
				indicators = new LocalJdbcTypeIndicators(
						// use ORDINAL as default for hbm.xml mappings
						getBoolean( NAMED, parameters ) ? STRING : ORDINAL,
						false,
						columnLength
				);
			}
			else {
				indicators = new LocalJdbcTypeIndicators(
						getEnumType( reader ),
						isNationalized( reader ),
						columnLength
				);
			}
			jdbcType = descriptor.getRecommendedJdbcType( indicators );
			isOrdinal = indicators.getEnumeratedType() != STRING;
		}

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Using %s-based conversion for Enum %s",
					isOrdinal() ? "ORDINAL" : "NAMED",
					enumClass.getName()
			);
		}
	}

	private jakarta.persistence.EnumType getEnumType(ParameterType reader) {
		if ( reader != null ) {
			if ( reader.isPrimaryKey() ) {
				final MapKeyEnumerated enumAnn = getAnnotation( reader.getAnnotationsMethod(), MapKeyEnumerated.class );
				if ( enumAnn != null ) {
					return enumAnn.value();
				}
			}
			final Enumerated enumAnn = getAnnotation( reader.getAnnotationsMethod(), Enumerated.class );
			if ( enumAnn != null ) {
				return enumAnn.value();
			}
		}
		return ORDINAL;
	}

	private boolean isNationalized(ParameterType reader) {
		return typeConfiguration.getCurrentBaseSqlTypeIndicators().isNationalized()
			|| reader!=null && getAnnotation( reader.getAnnotationsMethod(), Nationalized.class ) != null;
	}

	@SuppressWarnings("unchecked")
	private <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> annotationType) {
		for ( Annotation annotation : annotations ) {
			if ( annotationType.isInstance( annotation ) ) {
				return (A) annotation;
			}
		}
		return null;
	}

	@Override
	public int getSqlType() {
		verifyConfigured();
		return jdbcType.getJdbcTypeCode();
	}

	@Override
	public Class<T> returnedClass() {
		return enumClass;
	}

	@Override
	public boolean equals(T x, T y) throws HibernateException {
		return x == y;
	}

	@Override
	public int hashCode(T x) throws HibernateException {
		return x == null ? 0 : x.hashCode();
	}

	@Override
	public T nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		verifyConfigured();
		return jdbcType.getExtractor( enumJavaType ).extract( rs, position, session );
	}

	private void verifyConfigured() {
		if ( enumJavaType == null ) {
			throw new AssertionFailure("EnumType (" + enumClass.getName() + ") not properly, fully configured");
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, T value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		verifyConfigured();
		jdbcType.getBinder( enumJavaType ).bind( st, value, index, session );
	}

	@Override
	public T deepCopy(T value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(T value) throws HibernateException {
		return value;
	}

	@Override
	public T assemble(Serializable cached, Object owner) throws HibernateException {
		return (T) cached;
	}

	@Override
	public T replace(T original, T target, Object owner) throws HibernateException {
		return original;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public String toSqlLiteral(T value) {
		verifyConfigured();
		return isOrdinal()
				? Integer.toString( value.ordinal() )
				: "'" + value.name() + "'";
	}

	@Override
	public String toString(T value) {
		verifyConfigured();
		return enumJavaType.toName( value );
	}

	@Override
	public T fromStringValue(CharSequence sequence) {
		verifyConfigured();
		return enumJavaType.fromName( sequence.toString() );
	}

	@Override @SuppressWarnings("unchecked")
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		verifyConfigured();
		return enumJavaType.extractLoggableRepresentation( (T) value );
	}

	public boolean isOrdinal() {
		verifyConfigured();
		return isOrdinal;
	}

	private class LocalJdbcTypeIndicators implements JdbcTypeIndicators {
		private final jakarta.persistence.EnumType enumType;
		private final boolean nationalized;
		private final Long columnLength;

		private LocalJdbcTypeIndicators(jakarta.persistence.EnumType enumType, boolean nationalized, Long columnLength) {
			this.enumType = enumType;
			this.nationalized = nationalized;
			this.columnLength = columnLength;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public jakarta.persistence.EnumType getEnumeratedType() {
			return enumType != null ? enumType : typeConfiguration.getCurrentBaseSqlTypeIndicators().getEnumeratedType();
		}

		@Override
		public boolean isNationalized() {
			return nationalized;
		}


		@Override
		public long getColumnLength() {
			return columnLength == null ? NO_COLUMN_LENGTH : columnLength;
		}

		@Override
		public Dialect getDialect() {
			return typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
		}
	}
}
