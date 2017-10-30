/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-11117")
public class LazyBasicFieldMergeTest extends AbstractEnhancerTestTask {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                Company.class,
                Manager.class,
        };
    }

    public void prepare() {
        Configuration cfg = new Configuration();
        cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        super.prepare( cfg );
    }

    public void execute() {
        Session session = getFactory().openSession();
        session.beginTransaction();
        {
            Manager manager = new Manager();
            manager.setName("John Doe");
            manager.setResume(new byte[] {1, 2, 3});

            Company company = new Company();
            company.setName("Company");
            company.setManager(manager);

            Company _company = (Company) session.merge( company);
            assertEquals( company.getName(), _company.getName() );
            assertArrayEquals( company.getManager().getResume(), _company.getManager().getResume() );
        }
        session.getTransaction().commit();
        session.close();
    }

    protected void cleanup() {
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

        @OneToOne(mappedBy = "company", cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
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
