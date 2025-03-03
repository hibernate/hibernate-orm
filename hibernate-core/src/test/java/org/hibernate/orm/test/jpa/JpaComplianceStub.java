/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import org.hibernate.jpa.spi.JpaCompliance;

/**
 * Stubbed impl of JpaCompliance
 *
 * @author Steve Ebersole
 */
public class JpaComplianceStub implements JpaCompliance {
	@Override
	public boolean isJpaQueryComplianceEnabled() {
		return false;
	}

	@Override
	public boolean isJpaTransactionComplianceEnabled() {
		return false;
	}

	@Override
	public boolean isJpaCascadeComplianceEnabled() {
		return false;
	}

	@Override
	public boolean isJpaClosedComplianceEnabled() {
		return false;
	}

	@Override
	public boolean isJpaProxyComplianceEnabled() {
		return false;
	}

	@Override
	public boolean isJpaCacheComplianceEnabled() {
		return false;
	}

	@Override
	public boolean isGlobalGeneratorScopeEnabled() {
		return false;
	}

	@Override
	public boolean isJpaOrderByMappingComplianceEnabled() {
		return false;
	}

	@Override
	public boolean isLoadByIdComplianceEnabled() {
		return false;
	}
}
