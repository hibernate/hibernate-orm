/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.lazyload;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Vlad Mihalcea
 */
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.JPA_PROXY_COMPLIANCE, value = "true")
)
public class ManyToOneLazyLoadingByIdJpaComplianceTest extends ManyToOneLazyLoadingByIdTest {

	@Override
	protected void assertProxyState(Continent continent) {
		assertEquals( "Europe", continent.getName() );
	}
}
