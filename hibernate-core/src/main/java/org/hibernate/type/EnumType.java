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
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.Nationalized;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorIndicators;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;
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
public class EnumType<T extends Enum>
		implements EnhancedUserType, DynamicParameterizedType, LoggableUserType, TypeConfigurationAware, Serializable {
	private static final Logger LOG = CoreLogging.logger( EnumType.class );

	public static final String ENUM = "enumClass";
	public static final String NAMED = "useNamed";
	public static final String TYPE = "type";

	private Class enumClass;

	private EnumValueConverter enumValueConverter;

	private TypeConfiguration typeConfiguration;

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

			final EnumJavaTypeDescriptor enumJavaDescriptor = (EnumJavaTypeDescriptor) typeConfiguration
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( enumClass );

			final BasicJavaDescriptor<?> relationalJavaDescriptor = resolveRelationalJavaTypeDescriptor(
					reader,
					enumType,
					enumJavaDescriptor
			);

			final SqlTypeDescriptor sqlTypeDescriptor = relationalJavaDescriptor.getJdbcRecommendedSqlType(
					new LocalSqlTypeDescriptorIndicators( enumType, reader )
			);

			if ( isOrdinal ) {
				this.enumValueConverter = new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						sqlTypeDescriptor,
						relationalJavaDescriptor
				);
			}
			else {
				this.enumValueConverter = new NamedEnumValueConverter(
						enumJavaDescriptor,
						sqlTypeDescriptor,
						relationalJavaDescriptor
				);
			}
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
		}

		LOG.debugf(
				"Using %s-based conversion for Enum %s",
				isOrdinal() ? "ORDINAL" : "NAMED",
				enumClass.getName()
		);
	}

	private BasicJavaDescriptor<?> resolveRelationalJavaTypeDescriptor(
			ParameterType reader,
			javax.persistence.EnumType enumType, EnumJavaTypeDescriptor enumJavaDescriptor) {
		return enumJavaDescriptor.getJdbcRecommendedSqlType( new LocalSqlTypeDescriptorIndicators( enumType, reader ) )
				.getJdbcRecommendedJavaTypeMapping( typeConfiguration );
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

	private <A extends Annotation> A getAnnotation(Annotation[] annotations, Class<A> anClass) {
		for ( Annotation annotation : annotations ) {
			if ( anClass.isInstance( annotation ) ) {
				return (A) annotation;
			}
		}
		return null;
	}

	private EnumValueConverter interpretParameters(Properties parameters) {
		final EnumJavaTypeDescriptor enumJavaDescriptor = (EnumJavaTypeDescriptor) typeConfiguration
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( enumClass );

		final ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );
		final javax.persistence.EnumType enumType = getEnumType( reader );
		final LocalSqlTypeDescriptorIndicators localIndicators = new LocalSqlTypeDescriptorIndicators( enumType, reader );

		final BasicJavaDescriptor stringJavaDescriptor = (BasicJavaDescriptor) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( String.class );
		final BasicJavaDescriptor integerJavaDescriptor = (BasicJavaDescriptor) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class );

		if ( parameters.containsKey( NAMED ) ) {
			final boolean useNamed = ConfigurationHelper.getBoolean( NAMED, parameters );
			if ( useNamed ) {
				return new NamedEnumValueConverter(
						enumJavaDescriptor,
						stringJavaDescriptor.getJdbcRecommendedSqlType( localIndicators ),
						stringJavaDescriptor
				);
			}
			else {
				return new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						integerJavaDescriptor.getJdbcRecommendedSqlType( localIndicators ),
						(BasicJavaDescriptor) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class )
				);
			}
		}

		if ( parameters.containsKey( TYPE ) ) {
			final int type = Integer.decode( (String) parameters.get( TYPE ) );
			if ( isNumericType( type ) ) {
				return new OrdinalEnumValueConverter(
						enumJavaDescriptor,
						integerJavaDescriptor.getJdbcRecommendedSqlType( localIndicators ),
						(BasicJavaDescriptor) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class )
				);
			}
			else if ( isCharacterType( type ) ) {
				return new NamedEnumValueConverter(
						enumJavaDescriptor,
						stringJavaDescriptor.getJdbcRecommendedSqlType( localIndicators ),
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
				integerJavaDescriptor.getJdbcRecommendedSqlType( localIndicators ),
				(BasicJavaDescriptor) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Integer.class )
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
	public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws SQLException {
		throw new NotYetImplementedFor6Exception( getClass() );
//		verifyConfigured();
//		return enumValueConverter.readValue( rs, names[0], session );
	}

	private void verifyConfigured() {
		if ( enumValueConverter == null ) {
			throw new AssertionFailure( "EnumType (" + enumClass.getName() + ") not properly, fully configured" );
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		throw new NotYetImplementedFor6Exception( getClass() );
//		verifyConfigured();
//		enumValueConverter.writeValue( st, (Enum) value, index, session );
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
		return (String) enumValueConverter.getDomainJavaDescriptor().unwrap( (Enum) value, String.class, null );
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
		return enumValueConverter.getDomainJavaDescriptor().toString( (Enum) value );
	}

	public boolean isOrdinal() {
		verifyConfigured();
		return enumValueConverter instanceof OrdinalEnumValueConverter;
	}

	private class LocalSqlTypeDescriptorIndicators implements SqlTypeDescriptorIndicators {
		private final javax.persistence.EnumType enumType;
		private final ParameterType reader;

		public LocalSqlTypeDescriptorIndicators(javax.persistence.EnumType enumType, ParameterType reader) {
			this.enumType = enumType;
			this.reader = reader;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public javax.persistence.EnumType getEnumeratedType() {
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

			for ( Annotation annotation : reader.getAnnotationsMethod() ) {
				if ( annotation instanceof Nationalized ) {
					return true;
				}
			}

			return false;
		}
	}
}
