/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.query.sqm.CastType;
import org.hibernate.type.AdjustableBasicType;
import org.hibernate.type.ConvertedBasicType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.ProcedureParameterExtractionAware;
import org.hibernate.type.ProcedureParameterNamedBinder;
import org.hibernate.type.TrueFalseConverter;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.MappingContext;

/**
 * @author Christian Beikov
 */
public class ConvertedBasicTypeImpl<J> implements ConvertedBasicType<J>,
		AdjustableBasicType<J>, ProcedureParameterExtractionAware<J>, ProcedureParameterNamedBinder<J> {

	public static final String EXTERNALIZED_PREFIX = "convertedBasicType";
	public static final String[] NO_REG_KEYS = ArrayHelper.EMPTY_STRING_ARRAY;
	private static int count;

	private final String name;
	private final String description;
	private final BasicValueConverter<J, ?> converter;
	private final JdbcType jdbcType;
	private final int[] sqlTypes;
	private final ValueBinder<J> jdbcValueBinder;
	private final ValueExtractor<J> jdbcValueExtractor;
	private final JdbcLiteralFormatter<J> jdbcLiteralFormatter;

	public ConvertedBasicTypeImpl(
			String name,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter) {
		this(
				name,
				String.format(
						Locale.ROOT,
						"%s@%s(%s,%s)",
						EXTERNALIZED_PREFIX,
						++count,
						converter.getDomainJavaType().getJavaTypeClass().getName(),
						jdbcType.getDefaultSqlTypeCode()
				),
				jdbcType,
				converter
		);
	}

	@SuppressWarnings("unchecked")
	public ConvertedBasicTypeImpl(
			String name,
			String description,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter) {
		this.name = name;
		this.description = description;
		this.converter = converter;
		this.jdbcType = jdbcType;
		this.sqlTypes = new int[] { jdbcType.getDdlTypeCode() };
		//TODO: these type casts look completely bogus
		this.jdbcValueBinder = (ValueBinder<J>) jdbcType.getBinder( converter.getRelationalJavaType() );
		this.jdbcValueExtractor = (ValueExtractor<J>) jdbcType.getExtractor( converter.getRelationalJavaType() );
		this.jdbcLiteralFormatter = (JdbcLiteralFormatter<J>) jdbcType.getJdbcLiteralFormatter( converter.getRelationalJavaType() );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String[] getRegistrationKeys() {
		// irrelevant - these are created on-the-fly
		return NO_REG_KEYS;
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return converter;
	}

	@Override
	public ValueExtractor<J> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	@Override
	public ValueBinder<J> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public JavaType<?> getJdbcJavaType() {
		return converter.getRelationalJavaType();
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}
	public final JavaType<J> getJavaTypeDescriptor() {
		return converter.getDomainJavaType();
	}

	public final JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public final Class<?> getReturnedClass() {
		return converter.getDomainJavaType().getJavaTypeClass();
	}

	@Override
	public final int getColumnSpan(MappingContext mapping) throws MappingException {
		return 1;
	}

	@Override
	public final int[] getSqlTypeCodes(MappingContext mappingContext) throws MappingException {
		return sqlTypes;
	}

	@Override
	public final boolean isAssociationType() {
		return false;
	}

	@Override
	public final boolean isCollectionType() {
		return false;
	}

	@Override
	public final boolean isComponentType() {
		return false;
	}

	@Override
	public final boolean isEntityType() {
		return false;
	}

	@Override
	public final boolean isAnyType() {
		return false;
	}

	@Override
	public final boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@Override
	public final boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isEqual(Object one, Object another) {
		return converter.getDomainJavaType().areEqual( (J) one, (J) another );
	}

	@Override
	@SuppressWarnings("unchecked")
	public int getHashCode(Object x) {
		return converter.getDomainJavaType().extractHashCode( (J) x );
	}

	@Override
	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final int compare(Object x, Object y) {
		return converter.getDomainJavaType().getComparator().compare( (J) x, (J) y );
	}

	@Override
	public final boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return isDirty( old, current );
	}

	@Override
	public final boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	@Override
	public final boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	@Override
	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}

	@Override
	public void nullSafeSet(
			CallableStatement st,
			J value,
			String name,
			SharedSessionContractImplementor session) throws SQLException {
		final Object converted = converter.toRelationalValue( value );
		getJdbcValueBinder().bind( st, (J) converted, name, session );
	}

	@Override
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SharedSessionContractImplementor session) throws SQLException {
		//noinspection unchecked
		final Object converted = converter.toRelationalValue( (J) value );
		getJdbcValueBinder().bind( st, (J) converted, index, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( value ) ) {
			return  "<uninitialized>";
		}
		return converter.getDomainJavaType().extractLoggableRepresentation( (J) value );
	}

	protected MutabilityPlan<J> getMutabilityPlan() {
		return converter.getDomainJavaType().getMutabilityPlan();
	}

	@Override
	public final boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return getMutabilityPlan().deepCopy( (J) value );
	}

	@Override
	public final void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {
	}

	@Override
	public final Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( (J) value, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return converter.toRelationalValue( (J) value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache) {
		if ( original == null && target == null ) {
			return null;
		}

		return converter.getDomainJavaType().getReplacement( (J) original, (J) target, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map<Object, Object> copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
				? converter.getDomainJavaType().getReplacement( (J) original, (J) target, session )
				: target;
	}

	@Override
	public boolean canDoExtraction() {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public J extract(CallableStatement statement, int startIndex, final SharedSessionContractImplementor session) throws SQLException {
		return (J) getValueConverter().toDomainValue(
				getJdbcValueExtractor().extract(
						statement,
						startIndex,
						session
				)
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public J extract(CallableStatement statement, String paramName, final SharedSessionContractImplementor session) throws SQLException {
		return (J) getValueConverter().toDomainValue(
				getJdbcValueExtractor().extract(
						statement,
						paramName,
						session
				)
		);
	}

	@Override
	public boolean canDoSetting() {
		return true;
	}

	@Override
	public CastType getCastType() {
		final JdbcType jdbcType = getJdbcType();
		final int jdbcTypeCode = jdbcType.getDefaultSqlTypeCode();
		switch ( jdbcTypeCode ) {
			case Types.BIT:
			case Types.SMALLINT:
			case Types.TINYINT:
			case Types.INTEGER:
				if ( getJavaType() == Boolean.class ) {
					return CastType.INTEGER_BOOLEAN;
				}
				break;
			case Types.CHAR:
			case Types.NCHAR:
				// todo: check if we can remove this
				if ( getJavaType() == Boolean.class ) {
					if ( converter.getRelationalJavaType().getJavaType() == Character.class ) {
						if ( converter instanceof TrueFalseConverter ) {
							return CastType.TF_BOOLEAN;
						}
						else if ( converter instanceof YesNoConverter ) {
							return CastType.YN_BOOLEAN;
						}
					}
					return CastType.YN_BOOLEAN;
				}
				break;
			case Types.TIMESTAMP_WITH_TIMEZONE:
				if ( getJavaType() == ZonedDateTime.class ) {
					return CastType.ZONE_TIMESTAMP;
				}
				break;
		}
		return jdbcType.getCastType();
	}

	@Override
	public String toString() {
		return description;
	}

}
