/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jpa.JpaCompliance;

/**
 * @author Steve Ebersole
 */
public class JpaComplianceImpl implements JpaCompliance {
	private boolean queryCompliance;
	private boolean transactionCompliance;
	private boolean listCompliance;
	private boolean closedCompliance;


	@SuppressWarnings("ConstantConditions")
	public JpaComplianceImpl(Map configurationSettings, boolean jpaByDefault) {
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
}
