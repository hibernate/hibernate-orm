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
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.descriptor.converter.internal.NamedEnumValueConverter;
import org.hibernate.type.descriptor.converter.internal.OrdinalEnumValueConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
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

/**
 * Value type mapper for enumerations.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 *
 * @deprecated Use {@link ConvertedBasicType} instead
 */
@Deprecated(since="6.2", forRemoval=true)
public class EnumType<T extends Enum<T>>
		implements EnhancedUserType<T>, DynamicParameterizedType, LoggableUserType, TypeConfigurationAware, Serializable {
	private static final Logger LOG = CoreLogging.logger( EnumType.class );

	public static final String ENUM = "enumClass";
	public static final String NAMED = "useNamed";
	public static final String TYPE = "type";

	private Class<T> enumClass;

	private EnumValueConverter<T, Object> enumValueConverter;
	private JdbcType jdbcType;
	private ValueExtractor<Object> jdbcValueExtractor;
	private ValueBinder<Object> jdbcValueBinder;

	private TypeConfiguration typeConfiguration;

	public EnumType() {
	}

	@SuppressWarnings("unchecked")
	public EnumType(
			Class<T> enumClass,
			EnumValueConverter<T,?> enumValueConverter,
			TypeConfiguration typeConfiguration) {
		this.enumClass = enumClass;
		this.typeConfiguration = typeConfiguration;

		this.enumValueConverter = (EnumValueConverter<T,Object>) enumValueConverter;
		this.jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( enumValueConverter.getJdbcTypeCode() );
		this.jdbcValueExtractor = (ValueExtractor<Object>) jdbcType.getExtractor( enumValueConverter.getRelationalJavaType() );
		this.jdbcValueBinder = (ValueBinder<Object>) jdbcType.getBinder( enumValueConverter.getRelationalJavaType() );
	}

	public EnumValueConverter<T, ?> getEnumValueConverter() {
		return enumValueConverter;
	}

	@Override
	public JdbcType getJdbcType(TypeConfiguration typeConfiguration) {
		return jdbcType;
	}

	@Override
	public BasicValueConverter<T, Object> getValueConverter() {
		return enumValueConverter;
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

		// the `reader != null` block handles annotations, while the `else` block handles hbm.xml
		if ( reader != null ) {
			configureUsingReader( reader );
		}
		else {
			configureUsingParameters( parameters );
		}
		jdbcValueExtractor = jdbcType.getExtractor( enumValueConverter.getRelationalJavaType() );
		jdbcValueBinder = jdbcType.getBinder( enumValueConverter.getRelationalJavaType() );

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Using %s-based conversion for Enum %s",
					isOrdinal() ? "ORDINAL" : "NAMED",
					enumClass.getName()
			);
		}
	}

	@SuppressWarnings("unchecked")
	private void configureUsingParameters(Properties parameters) {
		final String enumClassName = (String) parameters.get( ENUM );
		try {
			enumClass = ReflectHelper.classForName( enumClassName, this.getClass() ).asSubclass( Enum.class );
		}
		catch ( ClassNotFoundException exception ) {
			throw new HibernateException( "Enum class not found: " + enumClassName, exception );
		}
		enumValueConverter = (EnumValueConverter<T,Object>) interpretParameters( parameters ); //this typecast is rubbish
		jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( enumValueConverter.getJdbcTypeCode() );
	}

	@SuppressWarnings({"rawtypes","unchecked"})
	private void configureUsingReader(ParameterType reader) {
		enumClass = (Class<T>) reader.getReturnedClass().asSubclass( Enum.class );
		final jakarta.persistence.EnumType enumType = getEnumType( reader );
		final JavaType<T> descriptor = typeConfiguration.getJavaTypeRegistry().getDescriptor( enumClass );
		final EnumJavaType<T> enumJavaType = (EnumJavaType<T>) descriptor;
		final LocalJdbcTypeIndicators indicators = new LocalJdbcTypeIndicators( enumType, reader );
		final JavaType<?> relationalJavaType = resolveRelationalJavaType( indicators, enumJavaType );
		jdbcType = relationalJavaType.getRecommendedJdbcType( indicators );
		enumValueConverter = isOrdinal( enumType )
				? new OrdinalEnumValueConverter( enumJavaType, jdbcType, relationalJavaType )
				: new NamedEnumValueConverter( enumJavaType, jdbcType, relationalJavaType );
	}

	private static boolean isOrdinal(jakarta.persistence.EnumType enumType) {
		if ( enumType == null ) {
			return true;
		}
		else {
			switch ( enumType ) {
				case ORDINAL:
					return true;
				case STRING:
					return false;
				default:
					throw new AssertionFailure( "Unknown EnumType: " + enumType);
			}
		}
	}

	private JavaType<? extends Number> resolveRelationalJavaType(
			LocalJdbcTypeIndicators indicators,
			EnumJavaType<?> enumJavaType) {
		return enumJavaType.getRecommendedJdbcType( indicators )
				.getJdbcRecommendedJavaTypeMapping( null, null, typeConfiguration );
	}

	private jakarta.persistence.EnumType getEnumType(ParameterType reader) {
		if ( reader == null ) {
			return null;
		}

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

		return null;
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

	private EnumValueConverter<T,?> interpretParameters(Properties parameters) {
		JavaType<T> javaType = typeConfiguration.getJavaTypeRegistry().getDescriptor( enumClass );
		final EnumJavaType<T> enumJavaType = (EnumJavaType<T>) javaType;

		// this method should only be called for hbm.xml handling
		assert parameters.get( PARAMETER_TYPE ) == null;

		final LocalJdbcTypeIndicators localIndicators = new LocalJdbcTypeIndicators(
				// use ORDINAL as default for hbm.xml mappings
				jakarta.persistence.EnumType.ORDINAL,
				// Is there a reasonable value here?  Limits the
				// number of enums that can be stored:
				// 	1 = 10
				//	2 = 100
				//  etc
				-1L,
				null
		);

		if ( parameters.containsKey( NAMED ) ) {
			final boolean useNamed = ConfigurationHelper.getBoolean( NAMED, parameters );
			return getConverter( enumJavaType, localIndicators, useNamed );
		}

		if ( parameters.containsKey( TYPE ) ) {
			final int type = Integer.decode( (String) parameters.get( TYPE ) );
			return getConverterForType( enumJavaType, localIndicators, type );
		}
		final JavaType<? extends Number> relationalJavaType = resolveRelationalJavaType( localIndicators, enumJavaType );
		// the fallback
		return new OrdinalEnumValueConverter<>(
				enumJavaType,
				relationalJavaType.getRecommendedJdbcType( localIndicators ),
				relationalJavaType
		);
	}

	private JavaType<String> getStringType() {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor(String.class);
	}

	private EnumValueConverter<T,?> getConverter(
			EnumJavaType<T> enumJavaType,
			EnumType<T>.LocalJdbcTypeIndicators localIndicators,
			boolean useNamed) {
		if (useNamed) {
			return new NamedEnumValueConverter<>(
					enumJavaType,
					getStringType().getRecommendedJdbcType( localIndicators ),
					getStringType()
			);
		}
		else {
			final JavaType<? extends Number> relationalJavaType = resolveRelationalJavaType( localIndicators, enumJavaType );
			return new OrdinalEnumValueConverter<>(
					enumJavaType,
					relationalJavaType.getRecommendedJdbcType( localIndicators ),
					relationalJavaType
			);
		}
	}

	private EnumValueConverter<T,?> getConverterForType(
			EnumJavaType<T> enumJavaType,
			LocalJdbcTypeIndicators localIndicators,
			int type) {
		if ( isNumericType(type) ) {
			final JavaType<? extends Number> relationalJavaType = resolveRelationalJavaType( localIndicators, enumJavaType );
			return new OrdinalEnumValueConverter<>(
					enumJavaType,
					relationalJavaType.getRecommendedJdbcType( localIndicators ),
					relationalJavaType
			);
		}
		else if ( isCharacterType(type) ) {
			return new NamedEnumValueConverter<>(
					enumJavaType,
					getStringType().getRecommendedJdbcType( localIndicators ),
					getStringType()
			);
		}
		else {
			throw new HibernateException(
					String.format( "Passed JDBC type code [%s] not recognized as numeric nor character", type )
			);
		}
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
	public int getSqlType() {
		verifyConfigured();
		return enumValueConverter.getJdbcTypeCode();
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
		return enumValueConverter.toDomainValue( jdbcValueExtractor.extract( rs, position, session ) );
	}

	private void verifyConfigured() {
		if ( enumValueConverter == null || jdbcValueBinder == null || jdbcValueExtractor == null ) {
			throw new AssertionFailure( "EnumType (" + enumClass.getName() + ") not properly, fully configured" );
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, T value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		verifyConfigured();
		jdbcValueBinder.bind( st, enumValueConverter.toRelationalValue( value ), index, session );
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
		return (Serializable) enumValueConverter.toRelationalValue( value );
	}

	@Override
	public T assemble(Serializable cached, Object owner) throws HibernateException {
		return enumValueConverter.toDomainValue( cached );
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
		return enumValueConverter.toSqlLiteral( value );
	}

	@Override
	public String toString(T value) {
		verifyConfigured();
		return enumValueConverter.getRelationalJavaType().toString( enumValueConverter.toRelationalValue( value ) );
	}

	@Override
	public T fromStringValue(CharSequence sequence) {
		verifyConfigured();
		return enumValueConverter.toDomainValue( enumValueConverter.getRelationalJavaType().fromString( sequence ) );
	}

	@Override @SuppressWarnings("unchecked")
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		verifyConfigured();
		return enumValueConverter.getDomainJavaType().toString( (T) value );
	}

	public boolean isOrdinal() {
		verifyConfigured();
		return enumValueConverter instanceof OrdinalEnumValueConverter;
	}

	private class LocalJdbcTypeIndicators implements JdbcTypeIndicators {
		private final jakarta.persistence.EnumType enumType;
		private final Long columnLength;
		private final ParameterType reader;

		private LocalJdbcTypeIndicators(jakarta.persistence.EnumType enumType, ParameterType reader) {
			this( enumType, reader.getColumnLengths()[0], reader );
		}

		private LocalJdbcTypeIndicators(jakarta.persistence.EnumType enumType, Long columnLength, ParameterType reader) {
			this.enumType = enumType;
			this.columnLength = columnLength;
			this.reader = reader;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public jakarta.persistence.EnumType getEnumeratedType() {
			if ( enumType != null ) {
				return enumType;
			}
			return typeConfiguration.getCurrentBaseSqlTypeIndicators().getEnumeratedType();
		}

		@Override
		public boolean isNationalized() {
			return isNationalized( reader );
		}

		private boolean isNationalized(ParameterType reader) {
			if ( typeConfiguration.getCurrentBaseSqlTypeIndicators().isNationalized() ) {
				return true;
			}

			if ( reader != null ) {
				for ( Annotation annotation : reader.getAnnotationsMethod() ) {
					if ( annotation instanceof Nationalized ) {
						return true;
					}
				}
			}

			return false;
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
