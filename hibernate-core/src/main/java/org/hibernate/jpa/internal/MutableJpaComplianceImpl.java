/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.jpa.spi.MutableJpaCompliance;

/**
 * @author Steve Ebersole
 */
public class MutableJpaComplianceImpl implements MutableJpaCompliance {
	private boolean listCompliance;
	private boolean orderByMappingCompliance;
	private boolean proxyCompliance;
	private boolean generatorNameScopeCompliance;
	private boolean queryCompliance;
	private boolean transactionCompliance;
	private boolean closedCompliance;
	private boolean cachingCompliance;
	private boolean loadByIdCompliance;
	private boolean cascadeCompliance;

	public MutableJpaComplianceImpl(Map<?,?> configurationSettings) {
		this(
				configurationSettings,
				ConfigurationHelper.getBoolean( AvailableSettings.JPA_COMPLIANCE, configurationSettings )
		);
	}

	@SuppressWarnings("ConstantConditions")
	public MutableJpaComplianceImpl(Map<?,?> configurationSettings, boolean jpaByDefault) {
		final Object legacyQueryCompliance = configurationSettings.get( AvailableSettings.JPAQL_STRICT_COMPLIANCE );

		cascadeCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_CASCADE_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		//noinspection deprecation
		listCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_LIST_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		proxyCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_PROXY_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		generatorNameScopeCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		orderByMappingCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_ORDER_BY_MAPPING_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		queryCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_QUERY_COMPLIANCE,
				configurationSettings,
				ConfigurationHelper.toBoolean( legacyQueryCompliance, jpaByDefault )
		);
		transactionCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_TRANSACTION_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		closedCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_CLOSED_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		cachingCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_CACHING_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		loadByIdCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_LOAD_BY_ID_COMPLIANCE,
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
		return cascadeCompliance;
	}

	@Override
	public boolean isJpaListComplianceEnabled() {
		return listCompliance;
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
	public void setListCompliance(boolean listCompliance) {
		this.listCompliance = listCompliance;
	}

	@Override
	public void setCascadeCompliance(boolean cascadeCompliance) {
		this.cascadeCompliance = cascadeCompliance;
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
		JpaComplianceImpl.JpaComplianceBuilder builder = new JpaComplianceImpl.JpaComplianceBuilder();
		builder = builder.setListCompliance( listCompliance )
				.setCascadeCompliance( cascadeCompliance )
				.setProxyCompliance( proxyCompliance )
				.setOrderByMappingCompliance( orderByMappingCompliance )
				.setGlobalGeneratorNameCompliance( generatorNameScopeCompliance )
				.setQueryCompliance( queryCompliance )
				.setTransactionCompliance( transactionCompliance )
				.setClosedCompliance( closedCompliance )
				.setCachingCompliance( cachingCompliance )
				.setLoadByIdCompliance( loadByIdCompliance );
		return builder.createJpaCompliance();
	}
}
