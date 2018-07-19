/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa;

import org.hibernate.jpa.spi.JpaCompliance;

/**
 * @author Steve Ebersole
 */
public class JpaComplianceTestingImpl implements JpaCompliance {
	public static JpaCompliance normal() {
		return new JpaComplianceTestingImpl();
	}

	public static JpaCompliance withTransactionCompliance() {
		final JpaComplianceTestingImpl compliance = new JpaComplianceTestingImpl();
		compliance.transactionCompliance = true;
		return compliance;
	}

	public static JpaCompliance withQueryCompliance() {
		final JpaComplianceTestingImpl compliance = new JpaComplianceTestingImpl();
		compliance.queryCompliance = true;
		return compliance;
	}

	public static JpaCompliance withListCompliance() {
		final JpaComplianceTestingImpl compliance = new JpaComplianceTestingImpl();
		compliance.listCompliance = true;
		return compliance;
	}

	public static JpaCompliance withClosedCompliance() {
		final JpaComplianceTestingImpl compliance = new JpaComplianceTestingImpl();
		compliance.closedCompliance = true;
		return compliance;
	}

	private boolean queryCompliance;
	private boolean transactionCompliance;
	private boolean listCompliance;
	private boolean closedCompliance;
	private boolean proxyCompliance;
	private boolean cacheCompliance;
	private boolean idGeneratorNameScopeCompliance;

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
		return cacheCompliance;
	}

	@Override
	public boolean isGlobalGeneratorScopeEnabled() {
		return idGeneratorNameScopeCompliance;
	}
}
