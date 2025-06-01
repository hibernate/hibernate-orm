/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.internal.UserTypeJavaTypeWrapper;
import org.hibernate.type.internal.UserTypeJdbcTypeAdapter;
import org.hibernate.type.internal.UserTypeVersionJavaTypeWrapper;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.EnhancedUserType;
import org.hibernate.usertype.LoggableUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.usertype.UserVersionType;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.type.descriptor.converter.internal.ConverterHelper.createValueConverter;

/**
 * Adapts {@link UserType} to the generic {@link Type} interface, in order
 * to isolate user code from changes in the internal Type contracts.
 *
 * @apiNote Many of the interfaces implemented here are implemented just to
 * handle the case of the wrapped type implementing them so we can pass them
 * along.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CustomType<J>
		extends AbstractType
		implements ConvertedBasicType<J>, ProcedureParameterNamedBinder<J>, ProcedureParameterExtractionAware<J> {

	private final UserType<J> userType;
	private final String[] registrationKeys;

	private final String name;

	private final JavaType<J> mappedJavaType;
	private final JavaType<?> jdbcJavaType;
	private final JdbcType jdbcType;

	private final ValueExtractor<J> valueExtractor;
	private final ValueBinder<J> valueBinder;
	private final JdbcLiteralFormatter<J> jdbcLiteralFormatter;

	private final BasicValueConverter<J, ?> converter;

	public CustomType(UserType<J> userType, TypeConfiguration typeConfiguration) throws MappingException {
		this( userType, EMPTY_STRING_ARRAY, typeConfiguration );
	}

	public CustomType(UserType<J> userType, String[] registrationKeys, TypeConfiguration typeConfiguration) {
		this.userType = userType;
		this.registrationKeys = registrationKeys;
		name = userType.getClass().getName();
		mappedJavaType = getMappedJavaType( userType );

		final AttributeConverter<J, ?> valueConverter = userType.getValueConverter();
		if ( valueConverter != null ) {
			converter = createValueConverter( valueConverter, typeConfiguration.getJavaTypeRegistry() );
			// When an explicit value converter is given,
			// we configure the custom type to use that instead of adapters that delegate to UserType.
			// This is necessary to support selecting a column with multiple domain type representations.
			jdbcType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( userType.getSqlType() );
			jdbcJavaType = converter.getRelationalJavaType();
			//noinspection unchecked
			valueExtractor = (ValueExtractor<J>) jdbcType.getExtractor( jdbcJavaType );
			//noinspection unchecked
			valueBinder = (ValueBinder<J>) jdbcType.getBinder( jdbcJavaType );
			//noinspection unchecked
			jdbcLiteralFormatter = (JdbcLiteralFormatter<J>) jdbcType.getJdbcLiteralFormatter( jdbcJavaType );
		}
		else {
			// create a JdbcType adapter that uses the UserType binder/extract handling
			jdbcType = new UserTypeJdbcTypeAdapter<>( userType, mappedJavaType );
			jdbcJavaType = jdbcType.getJdbcRecommendedJavaTypeMapping( null, null, typeConfiguration );
			valueExtractor = jdbcType.getExtractor( mappedJavaType );
			valueBinder = jdbcType.getBinder( mappedJavaType );
			jdbcLiteralFormatter =
					userType instanceof EnhancedUserType
							? jdbcType.getJdbcLiteralFormatter( mappedJavaType )
							: null;
			converter = null;
		}
	}

	private JavaType<J> getMappedJavaType(UserType<J> userType) {
		return userType instanceof UserVersionType<J> userVersionType
				? new UserTypeVersionJavaTypeWrapper<>( userVersionType, this )
				: new UserTypeJavaTypeWrapper<>( userType, this );
	}

	public UserType<J> getUserType() {
		return userType;
	}

	@Override
	public ValueExtractor<J> getJdbcValueExtractor() {
		return valueExtractor;
	}

	@Override
	public ValueBinder<J> getJdbcValueBinder() {
		return valueBinder;
	}

	@Override
	public JdbcLiteralFormatter<J> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public int[] getSqlTypeCodes(MappingContext mappingContext) {
		return new int[] { jdbcType.getDdlTypeCode() };
	}

	@Override
	public String[] getRegistrationKeys() {
		return registrationKeys;
	}

	@Override
	public int getColumnSpan(MappingContext session) {
		return 1;
	}

	@Override
	public Class<J> getReturnedClass() {
		return getUserType().returnedClass();
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return getUserType().equals( (J) x, (J) y );
	}

	@Override
	public int getHashCode(Object x) {
		return getUserType().hashCode( (J) x );
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) {
		final J assembled = getUserType().assemble( cached, owner );
		// Since UserType#assemble is an optional operation,
		// we have to handle the fact that it could produce a null value,
		// in which case we will try to use a converter for assembling,
		// or if that doesn't exist, simply use the relational value as is
		return assembled == null && cached != null
				? convertToDomainValue( cached )
				: assembled;
	}

	@Override
	public Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) {
		return disassembleForCache( value );
	}

	@Override
	public Serializable disassemble(Object value, SessionFactoryImplementor sessionFactory) {
		return disassembleForCache( value );
	}

	private Serializable disassembleForCache(Object value) {
		final Serializable disassembled = getUserType().disassemble( (J) value );
		// Since UserType#disassemble is an optional operation,
		// we have to handle the fact that it could produce a null value,
		// in which case we will try to use a converter for disassembling,
		// or if that doesn't exist, simply use the domain value as is
		return disassembled == null
				? (Serializable) convertToRelationalValue( (J) value )
				: disassembled;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		// Use the value converter if available for conversion to the jdbc representation
		return convertToRelationalValue( (J) value );
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {

		final Serializable disassembled = getUserType().disassemble( (J) value );
		// Since UserType#disassemble is an optional operation,
		// we have to handle the fact that it could produce a null value,
		// in which case we will try to use a converter for disassembling,
		// or if that doesn't exist, simply use the domain value as is
		if ( disassembled == null) {
			CacheHelper.addBasicValueToCacheKey( cacheKey, value, this, session );
		}
		else {
			cacheKey.addValue( disassembled );
			cacheKey.addHashCode( value == null ? 0 : getUserType().hashCode( (J) value ) );
		}
	}

	@Override
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache) {
		return getUserType().replace( (J) original, (J) target, owner );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws SQLException {
		if ( settable[0] ) {
			//noinspection unchecked
			getUserType().nullSafeSet( st, (J) value, index, session );
		}
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			SharedSessionContractImplementor session) throws SQLException {
		//noinspection unchecked
		getUserType().nullSafeSet( st, (J) value, index, session );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object deepCopy(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return getUserType().deepCopy( (J) value );
	}

	@Override
	public boolean isMutable() {
		return getUserType().isMutable();
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == null ) {
			return "null";
		}
		else if ( userType instanceof LoggableUserType loggableUserType ) {
			return loggableUserType.toLoggableString( value, factory );
		}
		else if ( userType instanceof EnhancedUserType<?> ) {
			return ( (EnhancedUserType<Object>) userType ).toString( value );
		}
		else {
			return value.toString();
		}
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mapping) {
		final boolean[] result = new boolean[ getColumnSpan(mapping) ];
		if ( value != null ) {
			Arrays.fill( result, true );
		}
		return result;
	}

	@Override
	public boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return checkable[0] && isDirty( old, current, session );
	}

	@Override
	public boolean canDoSetting() {
		return getUserType() instanceof ProcedureParameterNamedBinder<?> procedureParameterNamedBinder
			&& procedureParameterNamedBinder.canDoSetting();
	}

	@Override
	public void nullSafeSet(CallableStatement statement, J value, String name, SharedSessionContractImplementor session)
			throws SQLException {
		if ( canDoSetting() ) {
			//noinspection unchecked
			( (ProcedureParameterNamedBinder<J>) getUserType() )
					.nullSafeSet( statement, value, name, session );
		}
		else {
			throw new UnsupportedOperationException(
					"Type [" + getUserType() + "] does support parameter binding by name"
			);
		}
	}

	@Override
	public boolean canDoExtraction() {
		return getUserType() instanceof ProcedureParameterExtractionAware<?> procedureParameterExtractionAware
			&& procedureParameterExtractionAware.canDoExtraction();
	}

	@Override
	public J extract(CallableStatement statement, int startIndex, SharedSessionContractImplementor session)
			throws SQLException {
		if ( canDoExtraction() ) {
			//noinspection unchecked
			return ((ProcedureParameterExtractionAware<J>) getUserType() )
					.extract( statement, startIndex, session );
		}
		else {
			throw new UnsupportedOperationException(
					"Type [" + getUserType() + "] does support parameter value extraction"
			);
		}
	}

	@Override
	public J extract(CallableStatement statement, String paramName, SharedSessionContractImplementor session)
			throws SQLException {
		if ( canDoExtraction() ) {
			//noinspection unchecked
			return ((ProcedureParameterExtractionAware<J>) getUserType() )
					.extract( statement, paramName, session );
		}
		else {
			throw new UnsupportedOperationException(
					"Type [" + getUserType() + "] does support parameter value extraction"
			);
		}
	}

	@Override
	public int hashCode() {
		return getUserType().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof CustomType<?> customType
			&& getUserType().equals( customType.getUserType() );
	}

	@Override
	public Class<J> getJavaType() {
		return mappedJavaType.getJavaTypeClass();
	}

	@Override
	public JavaType<J> getMappedJavaType() {
		return mappedJavaType;
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return this.getMappedJavaType();
	}

	@Override
	public JavaType<J> getJavaTypeDescriptor() {
		return this.getMappedJavaType();
	}

	@Override
	public JavaType<?> getJdbcJavaType() {
		return jdbcJavaType;
	}

	@Override
	public BasicValueConverter<J, ?> getValueConverter() {
		return converter;
	}

}
