/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal;

import org.hibernate.jpa.spi.JpaCompliance;

/**
 * @author Andrea Boriero
 */
public class JpaComplianceImpl implements JpaCompliance {
	private final boolean orderByMappingCompliance;
	private final boolean proxyCompliance;
	private final boolean globalGeneratorNameScopeCompliance;
	private final boolean queryCompliance;
	private final boolean transactionCompliance;
	private final boolean closedCompliance;
	private final boolean cachingCompliance;
	private final boolean loadByIdCompliance;
	private final boolean cascadeCompliance;

	public JpaComplianceImpl(
			boolean orderByMappingCompliance,
			boolean proxyCompliance,
			boolean globalGeneratorNameScopeCompliance,
			boolean queryCompliance,
			boolean transactionCompliance,
			boolean closedCompliance,
			boolean cachingCompliance,
			boolean loadByIdCompliance,
			boolean cascadeCompliance) {
		this.queryCompliance = queryCompliance;
		this.transactionCompliance = transactionCompliance;
		this.closedCompliance = closedCompliance;
		this.proxyCompliance = proxyCompliance;
		this.cachingCompliance = cachingCompliance;
		this.globalGeneratorNameScopeCompliance = globalGeneratorNameScopeCompliance;
		this.orderByMappingCompliance = orderByMappingCompliance;
		this.loadByIdCompliance = loadByIdCompliance;
		this.cascadeCompliance = cascadeCompliance;
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
	public boolean isJpaCascadeComplianceEnabled() {
		return cascadeCompliance;
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

	@Override
	public boolean isJpaOrderByMappingComplianceEnabled() {
		return orderByMappingCompliance;
	}

	@Override
	public boolean isLoadByIdComplianceEnabled() {
		return loadByIdCompliance;
	}

	public static class JpaComplianceBuilder {
		private boolean queryCompliance;
		private boolean orderByMappingCompliance;
		private boolean proxyCompliance;
		private boolean globalGeneratorNameScopeCompliance;
		private boolean cachingCompliance;
		private boolean transactionCompliance;
		private boolean closedCompliance;
		private boolean loadByIdCompliance;
		private boolean cascadeCompliance;

		public JpaComplianceBuilder() {
		}

		public JpaComplianceBuilder setCascadeCompliance(boolean cascadeCompliance) {
			this.cascadeCompliance = cascadeCompliance;
			return this;
		}

		public JpaComplianceBuilder setOrderByMappingCompliance(boolean orderByMappingCompliance) {
			this.orderByMappingCompliance = orderByMappingCompliance;
			return this;
		}

		public JpaComplianceBuilder setProxyCompliance(boolean proxyCompliance) {
			this.proxyCompliance = proxyCompliance;
			return this;
		}

		public JpaComplianceBuilder setQueryCompliance(boolean queryCompliance) {
			this.queryCompliance = queryCompliance;
			return this;
		}

		public JpaComplianceBuilder setTransactionCompliance(boolean transactionCompliance) {
			this.transactionCompliance = transactionCompliance;
			return this;
		}

		public JpaComplianceBuilder setClosedCompliance(boolean closedCompliance) {
			this.closedCompliance = closedCompliance;
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

		public JpaComplianceBuilder setLoadByIdCompliance(boolean loadByIdCompliance) {
			this.loadByIdCompliance = loadByIdCompliance;
			return this;
		}

		JpaCompliance createJpaCompliance() {
			return new JpaComplianceImpl(
					orderByMappingCompliance,
					proxyCompliance,
					globalGeneratorNameScopeCompliance,
					queryCompliance,
					transactionCompliance,
					closedCompliance,
					cachingCompliance,
					loadByIdCompliance,
					cascadeCompliance
			);
		}
	}
}
