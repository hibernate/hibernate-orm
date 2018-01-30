/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that sensitive information is correctly masked.
 *
 * @author Bruno P. Kinoshita
 */
public class MaskSensitiveInformationTest extends BaseEntityManagerFunctionalTestCase {

    private EntityManagerFactory entityManagerFactory;

    private static final String GIVEN_TEST_VALUE = "";
    private static final String EXPECTED_MASKED_VALUE = "****";

    @Before
    public void setUp() {
        entityManagerFactory = entityManagerFactory();
    }

    @SuppressWarnings("unchecked")
    protected Map buildSettings() {
        @SuppressWarnings("rawtypes")
        Map settings = super.buildSettings();
        // these values are set in the hibernate.properties file already
        //settings.put( org.hibernate.cfg.AvailableSettings.USER, GIVEN_TEST_VALUE );
        //settings.put( org.hibernate.cfg.AvailableSettings.PASS, GIVEN_TEST_VALUE );
        settings.put( org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER, GIVEN_TEST_VALUE );
        settings.put( org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD, GIVEN_TEST_VALUE );
        return settings;
    }

    @Test
    public void testmaskOutSensitiveInformation() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap( SessionFactoryImpl.class );
        Map<String, Object> properties = sessionFactory.getProperties();
        assertEquals( EXPECTED_MASKED_VALUE, properties.get(org.hibernate.cfg.AvailableSettings.USER) );
        assertEquals( EXPECTED_MASKED_VALUE, properties.get(org.hibernate.cfg.AvailableSettings.PASS) );
        assertEquals( EXPECTED_MASKED_VALUE, properties.get(org.hibernate.cfg.AvailableSettings.JPA_JDBC_USER) );
        assertEquals( EXPECTED_MASKED_VALUE, properties.get(org.hibernate.cfg.AvailableSettings.JPA_JDBC_PASSWORD) );
    }
}
