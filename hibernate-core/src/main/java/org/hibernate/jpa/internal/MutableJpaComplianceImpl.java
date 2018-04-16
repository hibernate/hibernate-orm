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
	private boolean queryCompliance;
	private boolean transactionCompliance;
	private boolean listCompliance;
	private boolean closedCompliance;
	private boolean proxyCompliance;
	private boolean cachingCompliance;
	private final boolean globalGeneratorNameScopeCompliance;

	@SuppressWarnings("ConstantConditions")
	public MutableJpaComplianceImpl(Map configurationSettings, boolean jpaByDefault) {
		final Object legacyQueryCompliance = configurationSettings.get( AvailableSettings.JPAQL_STRICT_COMPLIANCE );

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
		listCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_LIST_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		closedCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_CLOSED_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		proxyCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_PROXY_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		cachingCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_CACHING_COMPLIANCE,
				configurationSettings,
				jpaByDefault
		);
		globalGeneratorNameScopeCompliance = ConfigurationHelper.getBoolean(
				AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE,
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
		return globalGeneratorNameScopeCompliance;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Mutators

	public void setQueryCompliance(boolean queryCompliance) {
		this.queryCompliance = queryCompliance;
	}

	public void setTransactionCompliance(boolean transactionCompliance) {
		this.transactionCompliance = transactionCompliance;
	}

	public void setListCompliance(boolean listCompliance) {
		this.listCompliance = listCompliance;
	}

	public void setClosedCompliance(boolean closedCompliance) {
		this.closedCompliance = closedCompliance;
	}

	public void setProxyCompliance(boolean proxyCompliance) {
		this.proxyCompliance = proxyCompliance;
	}

	public void setCachingCompliance(boolean cachingCompliance) {
		this.cachingCompliance = cachingCompliance;
	}

	@Override
	public JpaCompliance immutableCopy() {
		JpaComplianceImpl.JpaComplianceBuilder builder = new JpaComplianceImpl.JpaComplianceBuilder();
		builder.setQueryCompliance( queryCompliance )
				.setTransactionCompliance( transactionCompliance )
				.setListCompliance( listCompliance )
				.setClosedCompliance( closedCompliance )
				.setProxyCompliance( proxyCompliance )
				.setCachingCompliance( cachingCompliance )
				.setGlobalGeneratorNameCompliance( globalGeneratorNameScopeCompliance );
		return builder.createJpaCompliance();
	}
}
