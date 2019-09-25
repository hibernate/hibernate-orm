/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.Map;
import javax.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test that sensitive information is correctly masked.
 *
 * @author Bruno P. Kinoshita
 */
public class MaskSensitiveInformationTest extends BaseEntityManagerFunctionalTestCase {

	private EntityManagerFactory entityManagerFactory;

	private static final String EXPECTED_MASKED_VALUE = "****";

	@Before
	public void setUp() {
		entityManagerFactory = entityManagerFactory();
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
	}

	@Test
	public void testMaskOutSensitiveInformation() {
		SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap( SessionFactoryImpl.class );
		Map<String, Object> properties = sessionFactory.getProperties();
		assertEquals( EXPECTED_MASKED_VALUE, properties.get( AvailableSettings.USER ) );
		assertEquals( EXPECTED_MASKED_VALUE, properties.get( AvailableSettings.PASS ) );
		assertEquals( EXPECTED_MASKED_VALUE, properties.get( AvailableSettings.JPA_JDBC_USER ) );
		assertEquals( EXPECTED_MASKED_VALUE, properties.get( AvailableSettings.JPA_JDBC_PASSWORD ) );
	}
}
