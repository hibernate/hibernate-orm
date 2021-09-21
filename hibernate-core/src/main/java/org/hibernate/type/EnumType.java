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
import jakarta.persistence.Enumerated;
import jakarta.persistence.MapKeyEnumerated;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.annotations.Nationalized;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;

import org.jboss.logging.Logger;

/**
 * Value type mapper for enumerations.
 *
 * Provides 2 distinct forms of "configuration" - one for hbm.xml mapping and
 * another for annotation/orm.xml mapping triggered within the {@link #setParameterValues}
 * method
 *
 * Annotation based config relies on a {@link ParameterType} reference passed as
 * an entry in the parameter values under the key {@link #PARAMETER_TYPE}
 *
 * hbm.xml based config relies on a number of values from the parameters: <ul>
 *     <li>
 *         {@link #ENUM} - Name the enumeration class.
 *     </li>
 *     <li>
 *         {@link #NAMED} - Should enum be mapped via name.  Default is to map as ordinal.
 *     </li>
 *     <li>
 * 			{@link #TYPE} - JDBC type code (legacy alternative to {@link #NAMED})
 *     </li>
 * </ul>
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class EnumType<T extends Enum<T>>
		implements EnhancedUserType, DynamicParameterizedType, LoggableUserType, TypeConfigurationAware, Serializable {
	private static final Logger LOG = CoreLogging.logger( EnumType.class );

	public static final String ENUM = "enumClass";
	public static final String NAMED = "useNamed";
	public static final String TYPE = "type";

	private Class<T> enumClass;

	private EnumValueConverter<T,Object> enumValueConverter;
	private JdbcTypeDescriptor jdbcTypeDescriptor;
	private ValueExtractor<T> jdbcValueExtractor;
	private ValueBinder<T> jdbcValueBinder;

	private TypeConfiguration typeConfiguration;

	public EnumType() {
	}

	public EnumType(
			Class<T> enumClass,
			EnumValueConverter enumValueConverter,
			TypeConfiguration typeConfiguration) {
		this.enumClass = enumClass;
		this.typeConfiguration = typeConfiguration;

		this.enumValueConverter = enumValueConverter;
		this.jdbcTypeDescriptor = typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( enumValueConverter.getJdbcTypeCode() );
		this.jdbcValueExtractor = jdbcTypeDescriptor.getExtractor( enumValueConverter.getRelationalJavaDescriptor() );
		this.jdbcValueBinder = jdbcTypeDescriptor.getBinder( enumValueConverter.getRelationalJavaDescriptor() );
	}

	public EnumValueConverter getEnumValueConverter() {
		return enumValueConverter;
	}

	@Override
	public void setParameterValues(Properties parameters) {
		// IMPL NOTE: we handle 2 distinct cases here:
		// 		1) we are passed a ParameterType instance in the incoming Properties - generally
		//			speaking this indicates the annotation-binding case, and the passed ParameterType
		//			represents information about the attribute and annotation
		//		2) we are not passed a ParameterType - generally this indicates a hbm.xml binding case.
		final ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );

		// the `reader != null` block handles annotations, while the `else` block
		// handles hbm.xml
		if ( reader != null ) {
			enumClass = reader.getReturnedClass().asSubclass( Enum.class );

			final Long columnLength = reader.getColumnLengths()[0];

			final boolean isOrdinal;
			final jakarta.persistence.EnumType enumType = getEnumType( reader );
			if ( enumType == null ) {
				isOrdinal = true;
			}
			else if ( jakarta.persistence.EnumType.ORDINAL.equals( enumType ) ) {
				isOrdinal = true;
			}
			else if ( jakarta.persistence.EnumType.STRING.equals( enumType ) ) {
				isOrdinal = false;
			}
			else {
				throw new AssertionFailure( "Unknown EnumType: " + enumType );
			}

			final EnumJavaTypeDescriptor enumJavaDescriptor = (EnumJavaTypeDescriptor) typeConfiguration
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( enumClass );

			final LocalJdbcTypeDescriptorIndicators indicators = new LocalJdbcTypeDescriptorIndicators(
					enumType,
					columnLength,
					reader
			);

			final BasicJavaDescriptor<?> relationalJtd = resolveRelationalJavaTypeDescriptor(
					indicators,
					enumJavaDescriptor
			);

			final JdbcTypeDescriptor jdbcTypeDescriptor = relationalJtd.getRecommendedJdbcType( indicators );

			if ( isOrdinal ) {
				this.enumValueConverter = new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						jdbcTypeDescriptor,
						relationalJtd
				);
			}
			else {
				this.enumValueConverter = new NamedEnumValueConverter(
						enumJavaDescriptor,
						jdbcTypeDescriptor,
						relationalJtd
				);
			}
			this.jdbcTypeDescriptor = jdbcTypeDescriptor;
		}
		else {
			final String enumClassName = (String) parameters.get( ENUM );
			try {
				enumClass = ReflectHelper.classForName( enumClassName, this.getClass() ).asSubclass( Enum.class );
			}
			catch ( ClassNotFoundException exception ) {
				throw new HibernateException( "Enum class not found: " + enumClassName, exception );
			}

			this.enumValueConverter = interpretParameters( parameters );
			this.jdbcTypeDescriptor = typeConfiguration.getJdbcTypeDescriptorRegistry().getDescriptor( enumValueConverter.getJdbcTypeCode() );
		}
		this.jdbcValueExtractor = (ValueExtractor) jdbcTypeDescriptor.getExtractor( enumValueConverter.getRelationalJavaDescriptor() );
		this.jdbcValueBinder = (ValueBinder) jdbcTypeDescriptor.getBinder( enumValueConverter.getRelationalJavaDescriptor() );

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Using %s-based conversion for Enum %s",
					isOrdinal() ? "ORDINAL" : "NAMED",
					enumClass.getName()
			);
		}
	}

	private BasicJavaDescriptor<?> resolveRelationalJavaTypeDescriptor(
			LocalJdbcTypeDescriptorIndicators indicators,
			EnumJavaTypeDescriptor<?> enumJavaDescriptor) {
		return enumJavaDescriptor.getRecommendedJdbcType( indicators ).getJdbcRecommendedJavaTypeMapping( typeConfiguration );
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

	private <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> anClass) {
		for ( Annotation annotation : annotations ) {
			if ( anClass.isInstance( annotation ) ) {
				return (A) annotation;
			}
		}
		return null;
	}

	private EnumValueConverter<T,Object> interpretParameters(Properties parameters) {
		//noinspection rawtypes
		final EnumJavaTypeDescriptor enumJavaDescriptor = (EnumJavaTypeDescriptor) typeConfiguration
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( enumClass );

		// this method should only be called for hbm.xml handling
		assert parameters.get( PARAMETER_TYPE ) == null;

		final LocalJdbcTypeDescriptorIndicators localIndicators = new LocalJdbcTypeDescriptorIndicators(
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
		final BasicJavaDescriptor<?> stringJavaDescriptor = (BasicJavaDescriptor<?>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( String.class );
		final BasicJavaDescriptor<?> integerJavaDescriptor = (BasicJavaDescriptor<?>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class );

		if ( parameters.containsKey( NAMED ) ) {
			final boolean useNamed = ConfigurationHelper.getBoolean( NAMED, parameters );
			if ( useNamed ) {
				//noinspection rawtypes
				return new NamedEnumValueConverter(
						enumJavaDescriptor,
						stringJavaDescriptor.getRecommendedJdbcType( localIndicators ),
						stringJavaDescriptor
				);
			}
			else {
				//noinspection rawtypes
				return new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						integerJavaDescriptor.getRecommendedJdbcType( localIndicators ),
						typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class )
				);
			}
		}

		if ( parameters.containsKey( TYPE ) ) {
			final int type = Integer.decode( (String) parameters.get( TYPE ) );
			if ( isNumericType( type ) ) {
				//noinspection rawtypes
				return new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						integerJavaDescriptor.getRecommendedJdbcType( localIndicators ),
						typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class )
				);
			}
			else if ( isCharacterType( type ) ) {
				//noinspection rawtypes
				return new NamedEnumValueConverter(
						enumJavaDescriptor,
						stringJavaDescriptor.getRecommendedJdbcType( localIndicators ),
						stringJavaDescriptor
				);
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
		return new OrdinalEnumValueConverter(
				enumJavaDescriptor,
				integerJavaDescriptor.getRecommendedJdbcType( localIndicators ),
				typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class )
		);
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
		verifyConfigured();
		return new int[] { enumValueConverter.getJdbcTypeCode() };
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
	public Object nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		verifyConfigured();
		final Object relational = jdbcValueExtractor.extract( rs, position, session );
		return enumValueConverter.toDomainValue( relational );
	}

	private void verifyConfigured() {
		if ( enumValueConverter == null || jdbcValueBinder == null || jdbcValueExtractor == null ) {
			throw new AssertionFailure( "EnumType (" + enumClass.getName() + ") not properly, fully configured" );
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		verifyConfigured();
		enumValueConverter.writeValue( st, (T) value, index, session );
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
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void setTypeConfiguration(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public String objectToSQLString(Object value) {
		verifyConfigured();
		return enumValueConverter.toSqlLiteral( value );
	}

	@Override
	public String toXMLString(Object value) {
		verifyConfigured();
		return enumValueConverter.getDomainJavaDescriptor().unwrap( (T) value, String.class, null );
	}

	@Override
	@SuppressWarnings("RedundantCast")
	public Object fromXMLString(String xmlValue) {
		verifyConfigured();
		return (T) enumValueConverter.getDomainJavaDescriptor().wrap( xmlValue, null );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		verifyConfigured();
		return enumValueConverter.getDomainJavaDescriptor().toString( (T) value );
	}

	public boolean isOrdinal() {
		verifyConfigured();
		return enumValueConverter instanceof OrdinalEnumValueConverter;
	}

	private class LocalJdbcTypeDescriptorIndicators implements JdbcTypeDescriptorIndicators {
		private final jakarta.persistence.EnumType enumType;
		private final Long columnLength;
		private final ParameterType reader;

		public LocalJdbcTypeDescriptorIndicators(jakarta.persistence.EnumType enumType, Long columnLength, ParameterType reader) {
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
	}
}
