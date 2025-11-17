/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal;

import java.util.Map;

import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.jpa.spi.MutableJpaCompliance;

import static org.hibernate.cfg.JpaComplianceSettings.JPAQL_STRICT_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_CACHING_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_CLOSED_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_LOAD_BY_ID_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_PROXY_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_QUERY_COMPLIANCE;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_TRANSACTION_COMPLIANCE;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.toBoolean;

/**
 * @author Steve Ebersole
 */
public class MutableJpaComplianceImpl implements MutableJpaCompliance {
	private boolean orderByMappingCompliance;
	private boolean proxyCompliance;
	private boolean generatorNameScopeCompliance;
	private boolean queryCompliance;
	private boolean transactionCompliance;
	private boolean closedCompliance;
	private boolean cachingCompliance;
	private boolean loadByIdCompliance;

	public MutableJpaComplianceImpl(Map<?,?> configurationSettings) {
		this( configurationSettings,
				getBoolean( JPA_COMPLIANCE, configurationSettings ) );
	}

	@SuppressWarnings("ConstantConditions")
	public MutableJpaComplianceImpl(Map<?,?> configurationSettings, boolean jpaByDefault) {
		final Object legacyQueryCompliance = configurationSettings.get( JPAQL_STRICT_COMPLIANCE );

		proxyCompliance = getBoolean(
				JPA_PROXY_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		generatorNameScopeCompliance = getBoolean(
				JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		orderByMappingCompliance = getBoolean(
				JPA_ORDER_BY_MAPPING_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		queryCompliance = getBoolean(
				JPA_QUERY_COMPLIANCE,
				configurationSettings,
				toBoolean( legacyQueryCompliance, jpaByDefault )
		);
		transactionCompliance = getBoolean(
				JPA_TRANSACTION_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		closedCompliance = getBoolean(
				JPA_CLOSED_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		cachingCompliance = getBoolean(
				JPA_CACHING_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		loadByIdCompliance = getBoolean(
				JPA_LOAD_BY_ID_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
	}

	@Override
	public boolean isJpaQueryComplianceEnabled() {
		return queryCompliance;
	}

	@Override
	public boolean isJpaTransactionComplianceEnabled() {
		return transactionCompliance;
	}

	public boolean isJpaCascadeComplianceEnabled() {
		return true;
	}

	@Override
	public boolean isJpaClosedComplianceEnabled() {
		return closedCompliance;
	}

	@Override
	public boolean isJpaProxyComplianceEnabled() {
		return proxyCompliance;
	}

	@Override
	public boolean isJpaCacheComplianceEnabled() {
		return cachingCompliance;
	}

	@Override
	public boolean isGlobalGeneratorScopeEnabled() {
		return generatorNameScopeCompliance;
	}

	@Override
	public boolean isJpaOrderByMappingComplianceEnabled() {
		return orderByMappingCompliance;
	}

	@Override
	public boolean isLoadByIdComplianceEnabled() {
		return loadByIdCompliance;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutators

	@Override
	public void setCascadeCompliance(boolean cascadeCompliance) {
	}

	@Override
	public void setOrderByMappingCompliance(boolean orderByMappingCompliance) {
		this.orderByMappingCompliance = orderByMappingCompliance;
	}

	@Override
	public void setProxyCompliance(boolean proxyCompliance) {
		this.proxyCompliance = proxyCompliance;
	}

	@Override
	public void setGeneratorNameScopeCompliance(boolean enabled) {
		this.generatorNameScopeCompliance = enabled;
	}

	public void setQueryCompliance(boolean queryCompliance) {
		this.queryCompliance = queryCompliance;
	}

	@Override
	public void setTransactionCompliance(boolean transactionCompliance) {
		this.transactionCompliance = transactionCompliance;
	}

	@Override
	public void setClosedCompliance(boolean closedCompliance) {
		this.closedCompliance = closedCompliance;
	}

	@Override
	public void setCachingCompliance(boolean cachingCompliance) {
		this.cachingCompliance = cachingCompliance;
	}

	@Override
	public void setLoadByIdCompliance(boolean enabled) {
		this.loadByIdCompliance = enabled;
	}

	@Override
	public JpaCompliance immutableCopy() {
		return new JpaComplianceImpl.JpaComplianceBuilder()
				.setProxyCompliance( proxyCompliance )
				.setOrderByMappingCompliance( orderByMappingCompliance )
				.setGlobalGeneratorNameCompliance( generatorNameScopeCompliance )
				.setQueryCompliance( queryCompliance )
				.setTransactionCompliance( transactionCompliance )
				.setClosedCompliance( closedCompliance )
				.setCachingCompliance( cachingCompliance )
				.setLoadByIdCompliance( loadByIdCompliance )
				.createJpaCompliance();
	}
}
