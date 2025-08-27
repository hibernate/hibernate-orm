/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.spi;

/**
 * @author Steve Ebersole
 */
public interface MutableJpaCompliance extends JpaCompliance {
	/**
	 * @deprecated Always enabled.  See {@linkplain JpaCompliance#isJpaCascadeComplianceEnabled()}
	 */
	@Deprecated
	void setCascadeCompliance(boolean cascadeCompliance);

	void setOrderByMappingCompliance(boolean orderByCompliance);

	void setProxyCompliance(boolean proxyCompliance);

	void setQueryCompliance(boolean queryCompliance);

	void setTransactionCompliance(boolean transactionCompliance);

	void setClosedCompliance(boolean closedCompliance);

	void setCachingCompliance(boolean cachingCompliance);

	void setGeneratorNameScopeCompliance(boolean generatorScopeCompliance);

	void setLoadByIdCompliance(boolean enabled);

	JpaCompliance immutableCopy();
}
