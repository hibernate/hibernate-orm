/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.sqm.CastType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Convenience base class for {@link BasicType} implementations.
 * Packages a {@link JavaType} with a {@link JdbcType}.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractStandardBasicType<T>
		implements BasicType<T>, ProcedureParameterExtractionAware<T>, ProcedureParameterNamedBinder<T> {

	private final JdbcType jdbcType;
	private final JavaType<T> javaType;
	private final int[] sqlTypes;
	private final ValueBinder<T> jdbcValueBinder;
	private final ValueExtractor<T> jdbcValueExtractor;
	private final JdbcLiteralFormatter<T> jdbcLiteralFormatter;
	private final @Nullable Type typeForEqualsHashCode;
	private final Class<?> javaTypeClass;
	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> javatypeComparator;

	public AbstractStandardBasicType(JdbcType jdbcType, JavaType<T> javaType) {
		this.jdbcType = jdbcType;
		this.sqlTypes = new int[] { jdbcType.getDdlTypeCode() };
		this.javaType = javaType;

		this.jdbcValueBinder = jdbcType.getBinder( javaType );
		this.jdbcValueExtractor = jdbcType.getExtractor( javaType );
		this.jdbcLiteralFormatter = jdbcType.getJdbcLiteralFormatter( javaType );

		//A very simple dispatch optimisation, make these a constant:
		this.javaTypeClass = javaType.getJavaTypeClass();
		this.mutabilityPlan = javaType.getMutabilityPlan();
		this.javatypeComparator = javaType.getComparator();
		this.typeForEqualsHashCode = javaType.useObjectEqualsHashCode() ? null : this;
	}

	@Override
	public ValueExtractor<T> getJdbcValueExtractor() {
		return jdbcValueExtractor;
	}

	@Override
	public ValueBinder<T> getJdbcValueBinder() {
		return jdbcValueBinder;
	}

	@Override
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public Class<T> getJavaType() {
		return BasicType.super.getJavaType();
	}

	public T fromString(CharSequence string) {
		return javaType.fromString( string );
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return this.mutabilityPlan;
	}

	@Override
	public boolean[] toColumnNullness(Object value, MappingContext mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	@Override
	public String[] getRegistrationKeys() {
		return registerUnderJavaType()
				? new String[] { getName(), javaType.getTypeName() }
				: new String[] { getName() };
	}

	protected boolean registerUnderJavaType() {
		return false;
	}

	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final JavaType<T> getJavaTypeDescriptor() {
		return javaType;
	}

	public final JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public final Class<?> getReturnedClass() {
		return javaTypeClass;
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
		if ( one == another ) {
			return true;
		}
		else if ( one == null || another == null ) {
			return false;
		}
		else if ( typeForEqualsHashCode == null ) {
			return one.equals( another );
		}
		else {
			return javaType.areEqual( (T) one, (T) another );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public int getHashCode(Object x) {
		if ( typeForEqualsHashCode == null ) {
			return x.hashCode();
		}
		else {
			return javaType.extractHashCode( (T) x );
		}
	}

	@Override
	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	public @Nullable Type getTypeForEqualsHashCode() {
		return typeForEqualsHashCode;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final int compare(Object x, Object y) {
		return this.javatypeComparator.compare( (T) x, (T) y );
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
		// MutableMutabilityPlan.INSTANCE is a special plan for which we always have to assume the value is dirty,
		// because we can't actually copy a value, but have no knowledge about the mutability of the java type
		return getMutabilityPlan() == MutableMutabilityPlan.INSTANCE || !isSame( old, current );
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
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SharedSessionContractImplementor session) throws SQLException {
		//noinspection unchecked
		nullSafeSet( st, (T) value, index, (WrapperOptions) session );
	}

	protected void nullSafeSet(PreparedStatement st, T value, int index, WrapperOptions options) throws SQLException {
		getJdbcValueBinder().bind( st, value, index, options );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( value ) ) {
			return  "<uninitialized>";
		}
		return javaType.extractLoggableRepresentation( (T) value );
	}

	@Override
	public final boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return deepCopy( (T) value );
	}

	protected final T deepCopy(T value) {
		return getMutabilityPlan().deepCopy( value );
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( (T) value, session );
	}

	@Override
	public final Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached, session );
	}

	@Override
	public final void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map<Object, Object> copyCache) {
		if ( original == null && target == null ) {
			return null;
		}

		return javaType.getReplacement( (T) original, (T) target, session );
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
				? javaType.getReplacement( (T) original, (T) target, session )
				: target;
	}

	@Override
	public boolean canDoExtraction() {
		return true;
	}

	@Override
	public T extract(CallableStatement statement, int startIndex, final SharedSessionContractImplementor session) throws SQLException {
		return getJdbcValueExtractor().extract(
				statement,
				startIndex,
				session
		);
	}

	@Override
	public T extract(CallableStatement statement, String paramName, final SharedSessionContractImplementor session) throws SQLException {
		return getJdbcValueExtractor().extract(
				statement,
				paramName,
				session
		);
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session) throws SQLException {

	}

	@Override
	public void nullSafeSet(CallableStatement st, T value, String name, SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, name, (WrapperOptions) session );
	}

	@SuppressWarnings("unchecked")
	protected final void nullSafeSet(CallableStatement st, Object value, String name, WrapperOptions options) throws SQLException {
		getJdbcValueBinder().bind( st, (T) value, name, options );
	}

	@Override
	public boolean canDoSetting() {
		return true;
	}

	@Override
	public CastType getCastType() {
		// The following is only necessary because we interpret a model part, e.g.
		//
		// @JdbcTypeCode( Types.INTEGER )
		// Boolean bool;
		//
		// as BasicTypeImpl( IntegerJdbcType, BooleanJavaType ) instead of
		// as ConvertedBasicTypeImpl( NumericBooleanConverter ).
		//
		// Due to that, we have to handle some conversions in wrap/unwrap of BooleanJavaType
		// and the cast type determination here. Note that we interpret the converter in ConvertedBasicTypeImpl
		// to properly determine the correct cast type
		final JdbcType jdbcType = getJdbcType();
		final int jdbcTypeCode = jdbcType.getDdlTypeCode();
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
				if ( getJavaType() == Boolean.class ) {
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
}
