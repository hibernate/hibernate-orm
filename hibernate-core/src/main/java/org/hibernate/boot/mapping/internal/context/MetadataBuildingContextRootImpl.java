/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.model.internal.TemporalHelper;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.pipeline.internal.MappingResolutionServices;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

import static org.hibernate.boot.model.internal.AuditHelper.determineAuditStrategy;

/**
 * Root {@link MetadataBuildingContext}.
 */
public class MetadataBuildingContextRootImpl implements MetadataBuildingContext {
	private final String contributor;
	private final BootstrapContext bootstrapContext;
	private final MappingResolutionServices serviceComponents;
	private final MappingResolutionOptions buildingPlan;
	private final EffectiveMappingDefaults mappingDefaults;
	private final InFlightMetadataCollector metadataCollector;
	private final ObjectNameNormalizer objectNameNormalizer;
	private final TypeDefinitionRegistryStandardImpl typeDefinitionRegistry;
	private final TemporalTableStrategy temporalTableStrategy;
	private final AuditStrategy auditStrategy;
	private final Map<String, BasicType<?>> adHocBasicTypeRegistrations = new HashMap<>();

	public MetadataBuildingContextRootImpl(MetadataBuildingContextRootInput input) {
		this.contributor = input.contributor();
		this.bootstrapContext = input.bootstrapContext();
		this.serviceComponents = input.serviceComponents();
		this.buildingPlan = input.buildingPlan();
		this.mappingDefaults = input.mappingDefaults();
		this.metadataCollector = input.metadataCollector();
		this.objectNameNormalizer = new ObjectNameNormalizer(this);
		this.typeDefinitionRegistry = new TypeDefinitionRegistryStandardImpl();
		this.temporalTableStrategy = temporalTableStrategy( serviceComponents );
		this.auditStrategy = auditStrategy( serviceComponents );
	}

	private TemporalTableStrategy temporalTableStrategy(MappingResolutionServices serviceComponents) {
		final var settings = serviceComponents.getConfigurationService().getSettings();
		return TemporalHelper.determineTemporalTableStrategy( settings );
	}

	private AuditStrategy auditStrategy(MappingResolutionServices serviceComponents) {
		final var settings = serviceComponents.getConfigurationService().getSettings();
		return determineAuditStrategy( settings );
	}

	@Override
	public BootstrapContext getBootstrapContext() {
		return bootstrapContext;
	}

	@Override
	public MappingResolutionServices getServiceComponents() {
		return serviceComponents;
	}

	@Override
	public MappingResolutionOptions getBuildingPlan() {
		return buildingPlan;
	}

	@Override
	public EffectiveMappingDefaults getEffectiveDefaults() {
		return mappingDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return metadataCollector;
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return objectNameNormalizer;
	}

	@Override
	public TypeDefinitionRegistryStandardImpl getTypeDefinitionRegistry() {
		return typeDefinitionRegistry;
	}

	@Override
	public void registerAdHocBasicType(BasicType<?> basicType) {
		adHocBasicTypeRegistrations.put( basicType.getName(), basicType );
	}

	@Override
	public <T> BasicType<T> resolveAdHocBasicType(String key) {
		//noinspection unchecked
		return (BasicType<T>) adHocBasicTypeRegistrations.get( key );
	}

	@Override
	public <T> BasicType<T> findAdHocBasicType(JavaType<T> javaType, JdbcType jdbcType) {
		for ( BasicType<?> basicType : adHocBasicTypeRegistrations.values() ) {
			if ( basicType.getClass() == BasicTypeImpl.class
					&& basicType.getJavaTypeDescriptor() == javaType
					&& basicType.getJdbcType() == jdbcType ) {
				//noinspection unchecked
				return (BasicType<T>) basicType;
			}
		}

		return null;
	}

	@Override
	public String getCurrentContributorName() {
		return contributor;
	}

	@Override
	public TemporalTableStrategy getTemporalTableStrategy() {
		final var dialect = getMetadataCollector().getDatabase().getDialect();
		return switch ( temporalTableStrategy ) {
			case AUTO -> dialect.getTemporalTableSupport().getDefaultTemporalTableStrategy();
			case SINGLE_TABLE, HISTORY_TABLE -> temporalTableStrategy;
			case NATIVE ->
					dialect.getTemporalTableSupport().supportsNativeTemporalTables()
							? TemporalTableStrategy.NATIVE
							: TemporalTableStrategy.HISTORY_TABLE;
		};
	}

	@Override
	public AuditStrategy getAuditStrategy() {
		return auditStrategy;
	}
}
