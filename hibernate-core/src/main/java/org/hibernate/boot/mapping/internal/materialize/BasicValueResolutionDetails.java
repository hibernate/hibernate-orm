/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.boot.mapping.internal.materialize.BasicValueResolutionBuilder.BasicValueRole;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.mapping.internal.sources.BasicValueSource;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

/// Mutable input used to build and apply a [BasicValue.Resolution].
///
/// The input bridges the simple mapping object and the richer binding or
/// materialization state.  It wraps the target [BasicValue], the
/// declarative [BasicValueSource], and any facts already discovered by
/// the binder/materializer, such as an explicit Java type, explicit JDBC
/// type, converter, mutability plan, temporal precision, or configured JDBC
/// type code.
///
/// @since 9.0
/// @author Steve Ebersole
public class BasicValueResolutionDetails {
	private final BasicValue value;
	private final BasicValueSource source;
	private final BasicValueRole role;
	private final String ownerName;
	private final String propertyName;
	private final boolean softDelete;
	private final SoftDeleteType softDeleteStrategy;
	private Properties typeParameters;
	private String explicitTypeName;
	private ConverterDescriptor<?,?> attributeConverterDescriptor;
	private org.hibernate.annotations.TimeZoneStorageType timeZoneStorageType;
	private jakarta.persistence.EnumType enumerationStyle;
	private jakarta.persistence.TemporalType temporalPrecision;
	private java.lang.reflect.Type resolvedJavaType;
	private BasicJavaType<?> explicitJavaType;
	private JdbcType explicitJdbcType;
	private MutabilityPlan<?> explicitMutabilityPlan;
	private Integer jdbcTypeCode;
	private boolean attributeImmutable;
	private Class<? extends MutabilityPlan<?>> attributeMutabilityPlanClass;

	private BasicValueResolutionDetails(
			BasicValue value,
			BasicValueSource source) {
		this.value = value;
		this.source = source;
		this.role = value.isVersion() ? BasicValueRole.VERSION : BasicValueRole.from( source.kind() );
		this.softDelete = value.isSoftDelete();
		this.softDeleteStrategy = value.getSoftDeleteStrategy();
		this.ownerName = value.getOwnerName();
		this.propertyName = value.getPropertyName();
	}

	public static BasicValueResolutionDetails create(BasicValue value, BasicValueSource source) {
		return new BasicValueResolutionDetails( value, source );
	}

	public BasicValue value() {
		return value;
	}

	public BasicValueSource source() {
		return source;
	}

	public MemberDetails member() {
		return source.member();
	}

	public BasicValueRole role() {
		return role;
	}

	public BasicValueSource.Kind kind() {
		return source.kind();
	}

	public BasicValue.SourceJavaType getSourceJavaType() {
		return source.sourceJavaType();
	}

	public Properties getTypeParameters() {
		return typeParameters;
	}

	public void setTypeParameters(Properties typeParameters) {
		this.typeParameters = typeParameters;
	}

	public void setTypeParameters(Map<String, ?> typeParameters) {
		final var properties = new Properties();
		properties.putAll( typeParameters );
		setTypeParameters( properties );
	}

	public DynamicParameterizedType.ParameterType createParameterType(ClassLoaderService classLoaderService) {
		return value.createResolutionParameterType( classLoaderService );
	}

	public String getExplicitTypeName() {
		return explicitTypeName;
	}

	public void setExplicitTypeName(String explicitTypeName) {
		this.explicitTypeName = explicitTypeName;
	}

	public Class<? extends UserType<?>> getExplicitCustomType() {
		return value.getExplicitCustomType();
	}

	public Properties buildCustomTypeProperties() {
		return value.buildCustomTypeProperties();
	}

	public ConverterDescriptor<?, ?> getAttributeConverterDescriptor() {
		return attributeConverterDescriptor == null
				? value.getJpaAttributeConverterDescriptor()
				: attributeConverterDescriptor;
	}

	public void setAttributeConverterDescriptor(ConverterDescriptor<?, ?> attributeConverterDescriptor) {
		this.attributeConverterDescriptor = attributeConverterDescriptor;
	}

	public boolean isVersion() {
		return role == BasicValueRole.VERSION;
	}

	public org.hibernate.annotations.TimeZoneStorageType getTimeZoneStorageType() {
		return timeZoneStorageType;
	}

	public void setTimeZoneStorageType(org.hibernate.annotations.TimeZoneStorageType timeZoneStorageType) {
		this.timeZoneStorageType = timeZoneStorageType;
	}

	public boolean isSoftDelete() {
		return softDelete;
	}

	public SoftDeleteType getSoftDeleteStrategy() {
		return softDeleteStrategy;
	}

	public jakarta.persistence.EnumType getEnumerationStyle() {
		return enumerationStyle;
	}

	public void setEnumerationStyle(jakarta.persistence.EnumType enumerationStyle) {
		this.enumerationStyle = enumerationStyle;
	}

	public java.lang.reflect.Type getResolvedJavaType() {
		return resolvedJavaType;
	}

	public void setResolvedJavaType(java.lang.reflect.Type resolvedJavaType) {
		this.resolvedJavaType = resolvedJavaType;
	}

	public BasicJavaType<?> explicitJavaType() {
		return explicitJavaType;
	}

	public void setExplicitJavaType(BasicJavaType<?> explicitJavaType) {
		this.explicitJavaType = explicitJavaType;
	}

	public JdbcType explicitJdbcType() {
		return explicitJdbcType;
	}

	public void setExplicitJdbcType(JdbcType explicitJdbcType) {
		this.explicitJdbcType = explicitJdbcType;
	}

	public MutabilityPlan<?> explicitMutabilityPlan() {
		return explicitMutabilityPlan;
	}

	public void setExplicitMutabilityPlan(MutabilityPlan<?> explicitMutabilityPlan) {
		this.explicitMutabilityPlan = explicitMutabilityPlan;
	}

	public Table getTable() {
		return value.getTable();
	}

	public Selectable getColumn() {
		return value.getColumn();
	}

	public String getOwnerName() {
		return ownerName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public Integer getConfiguredJdbcTypeCode() {
		return jdbcTypeCode;
	}

	public void setConfiguredJdbcTypeCode(Integer jdbcTypeCode) {
		this.jdbcTypeCode = jdbcTypeCode;
	}

	public boolean attributeImmutable() {
		return attributeImmutable;
	}

	public void markAttributeImmutable() {
		this.attributeImmutable = true;
	}

	public Class<? extends MutabilityPlan<?>> attributeMutabilityPlanClass() {
		return attributeMutabilityPlanClass;
	}

	public void setAttributeMutabilityPlanClass(Class<? extends MutabilityPlan<?>> attributeMutabilityPlanClass) {
		this.attributeMutabilityPlanClass = attributeMutabilityPlanClass;
	}

	public jakarta.persistence.EnumType getEnumeratedType() {
		return enumerationStyle;
	}

	public jakarta.persistence.TemporalType getTemporalPrecision() {
		return temporalPrecision;
	}

	public void setTemporalPrecision(jakarta.persistence.TemporalType temporalPrecision) {
		this.temporalPrecision = temporalPrecision;
	}

	public boolean isNationalized() {
		return value.isNationalized();
	}

	public boolean isLob() {
		return value.isLob();
	}

	public long getColumnLength() {
		return value.getColumnLength();
	}

	public int getColumnPrecision() {
		return value.getColumnPrecision();
	}

	public int getColumnScale() {
		return value.getColumnScale();
	}
}
