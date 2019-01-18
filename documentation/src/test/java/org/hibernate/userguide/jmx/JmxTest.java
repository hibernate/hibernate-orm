/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.jmx;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class JmxTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class,
        };
    }

    @Override
    protected Map buildSettings() {
        Map properties = super.buildSettings();
        properties.put( AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString());
        properties.put( AvailableSettings.JMX_ENABLED, Boolean.TRUE.toString());
        properties.put( AvailableSettings.JMX_DOMAIN_NAME, "test");
        return properties;
    }

    @Test
    @TestForIssue( jiraKey = "HHH-7405" )
    public void test() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            Person person = new Person();
            person.id = 1L;
            entityManager.persist(person);
        });
    }

    @Entity(name = "Person")
    public static class Person  {

        @Id
        private Long id;

        private String firstName;

        private String lastName;
    }
}
