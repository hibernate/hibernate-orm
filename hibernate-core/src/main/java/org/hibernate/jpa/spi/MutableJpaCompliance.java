/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.spi;

/**
 * @author Steve Ebersole
 */
public interface MutableJpaCompliance extends JpaCompliance {
	void setCascadeCompliance(boolean cascadeCompliance);

	void setListCompliance(boolean listCompliance);

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
