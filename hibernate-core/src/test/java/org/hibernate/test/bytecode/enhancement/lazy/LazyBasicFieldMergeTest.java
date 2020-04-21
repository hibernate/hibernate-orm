/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.NonIdentifierAttribute;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-11117")
@RunWith( BytecodeEnhancerRunner.class )
public class LazyBasicFieldMergeTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                Company.class,
                Manager.class,
        };
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, session -> {
            Manager manager = new Manager();
            manager.setName("John Doe");
            manager.setResume(new byte[] {1, 2, 3});

            Company company = new Company();
            company.setName("Company");
            company.setManager(manager);

            Company _company = (Company) session.merge( company);
            assertEquals( company.getName(), _company.getName() );
            assertArrayEquals( company.getManager().getResume(), _company.getManager().getResume() );
        } );
    }

    @Entity(name = "Company")
    @Table(name = "COMPANY")
    public static class Company {

        @Id
        @GeneratedValue
        @Column(name = "COMP_ID")
        private Long id;

        @Column(name = "NAME")
        private String name;

        @OneToOne(mappedBy = "company", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
        private Manager manager;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Manager getManager() {
            return manager;
        }

        public void setManager(Manager manager) {
            this.manager = manager;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }


    @Entity(name = "Manager")
    @Table(name = "MANAGER")
    public static class Manager {

        @Id
        @GeneratedValue
        @Column(name = "MAN_ID")
        private Long id;

        @Column(name = "NAME")
        private String name;

        @Lob
        @Column(name = "RESUME")
        @Basic(fetch = FetchType.LAZY)
        private byte[] resume;

        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "COMP_ID")
        private Company company;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public byte[] getResume() {
            return resume;
        }

        public void setResume(byte[] resume) {
            this.resume = resume;
        }

        public Company getCompany() {
            return company;
        }

        public void setCompany(Company company) {
            this.company = company;
        }
    }
}
