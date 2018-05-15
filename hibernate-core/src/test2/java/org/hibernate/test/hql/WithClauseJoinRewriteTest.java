/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.Version;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of WithClauseTest.
 *
 * @author Steve Ebersole
 */
public class WithClauseJoinRewriteTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                AbstractObject.class,
                AbstractConfigurationObject.class,
                ConfigurationObject.class
        };
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11230")
    public void testInheritanceReAliasing() {
        Session s = openSession();
        Transaction tx = s.beginTransaction();

        // Just assert that the query is successful
        List<Object[]> results = s.createQuery(
                "SELECT usedBy.id, usedBy.name, COUNT(inverse.id) " +
                "FROM " + AbstractConfigurationObject.class.getName() + " config " +
                "INNER JOIN config.usedBy usedBy " +
                "LEFT JOIN usedBy.uses inverse ON inverse.id = config.id " +
                "WHERE config.id = 0 " +
                "GROUP BY usedBy.id, usedBy.name",
                Object[].class
        ).getResultList();

        tx.commit();
        s.close();
    }

    @Entity
    @Table( name = "config" )
    @Inheritance( strategy = InheritanceType.JOINED )
    public static abstract class AbstractConfigurationObject<T extends ConfigurationObject> extends AbstractObject {

        private String name;
        private Set<ConfigurationObject> uses = new HashSet<>();
        private Set<ConfigurationObject> usedBy = new HashSet<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @ManyToMany( targetEntity = AbstractConfigurationObject.class, fetch = FetchType.LAZY, cascade = {} )
        public Set<ConfigurationObject> getUses () {
            return uses;
        }

        public void setUses(Set<ConfigurationObject> uses) {
            this.uses = uses;
        }

        @ManyToMany ( targetEntity = AbstractConfigurationObject.class, fetch = FetchType.LAZY, mappedBy = "uses", cascade = {} )
        public Set<ConfigurationObject> getUsedBy () {
            return usedBy;
        }

        public void setUsedBy(Set<ConfigurationObject> usedBy) {
            this.usedBy = usedBy;
        }
    }

    @Entity
    @Table( name = "config_config" )
    public static class ConfigurationObject extends AbstractConfigurationObject<ConfigurationObject> {

    }

    @MappedSuperclass
    public static class AbstractObject {

        private Long id;
        private Long version;

        @Id
        @GeneratedValue
        public Long getId () {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Version
        @Column( nullable = false )
        public Long getVersion () {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }
    }

}
