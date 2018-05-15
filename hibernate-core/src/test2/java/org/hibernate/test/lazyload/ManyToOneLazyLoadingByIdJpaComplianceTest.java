/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class ManyToOneLazyLoadingByIdJpaComplianceTest extends ManyToOneLazyLoadingByIdTest {

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.JPA_PROXY_COMPLIANCE, Boolean.TRUE );
	}

	@Override
	protected void assertProxyState(Continent continent) {
		assertEquals( "Europe", continent.getName() );
	}
}
