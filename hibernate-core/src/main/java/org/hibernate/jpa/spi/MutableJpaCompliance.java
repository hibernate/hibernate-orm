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
	void setQueryCompliance(boolean queryCompliance);

	void setTransactionCompliance(boolean transactionCompliance);

	void setListCompliance(boolean listCompliance);

	void setClosedCompliance(boolean closedCompliance);

	void setProxyCompliance(boolean proxyCompliance);

	void setCachingCompliance(boolean cachingCompliance);

	JpaCompliance immutableCopy();
}
