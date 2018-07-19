/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal;

import org.hibernate.jpa.spi.JpaCompliance;

/**
 * @author Andrea Boriero
 */
public class JpaComplianceImpl implements JpaCompliance {
	private boolean queryCompliance;
	private boolean transactionCompliance;
	private boolean listCompliance;
	private boolean closedCompliance;
	private boolean proxyCompliance;
	private boolean cachingCompliance;
	private boolean globalGeneratorNameScopeCompliance;

	private JpaComplianceImpl(
			boolean queryCompliance,
			boolean transactionCompliance,
			boolean listCompliance,
			boolean closedCompliance,
			boolean proxyCompliance,
			boolean cachingCompliance,
			boolean globalGeneratorNameScopeCompliance) {
		this.queryCompliance = queryCompliance;
		this.transactionCompliance = transactionCompliance;
		this.listCompliance = listCompliance;
		this.closedCompliance = closedCompliance;
		this.proxyCompliance = proxyCompliance;
		this.cachingCompliance = cachingCompliance;
		this.globalGeneratorNameScopeCompliance = globalGeneratorNameScopeCompliance;
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

	public static class JpaComplianceBuilder {
		private boolean queryCompliance;
		private boolean transactionCompliance;
		private boolean listCompliance;
		private boolean closedCompliance;
		private boolean proxyCompliance;
		private boolean cachingCompliance;
		private boolean globalGeneratorNameScopeCompliance;

		public JpaComplianceBuilder() {
		}

		public JpaComplianceBuilder setQueryCompliance(boolean queryCompliance) {
			this.queryCompliance = queryCompliance;
			return this;
		}

		public JpaComplianceBuilder setTransactionCompliance(boolean transactionCompliance) {
			this.transactionCompliance = transactionCompliance;
			return this;
		}

		public JpaComplianceBuilder setListCompliance(boolean listCompliance) {
			this.listCompliance = listCompliance;
			return this;
		}

		public JpaComplianceBuilder setClosedCompliance(boolean closedCompliance) {
			this.closedCompliance = closedCompliance;
			return this;
		}

		public JpaComplianceBuilder setProxyCompliance(boolean proxyCompliance) {
			this.proxyCompliance = proxyCompliance;
			return this;
		}

		public JpaComplianceBuilder setCachingCompliance(boolean cachingCompliance) {
			this.cachingCompliance = cachingCompliance;
			return this;
		}

		public JpaComplianceBuilder setGlobalGeneratorNameCompliance(boolean globalGeneratorNameCompliance) {
			this.globalGeneratorNameScopeCompliance = globalGeneratorNameCompliance;
			return this;
		}

		JpaCompliance createJpaCompliance() {
			return new JpaComplianceImpl(
					queryCompliance,
					transactionCompliance,
					listCompliance,
					closedCompliance,
					proxyCompliance,
					cachingCompliance,
					globalGeneratorNameScopeCompliance
			);
		}
	}
}
