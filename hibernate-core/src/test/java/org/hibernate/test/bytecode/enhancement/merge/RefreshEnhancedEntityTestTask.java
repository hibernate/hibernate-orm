/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.merge;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Barreiro
 */
public class RefreshEnhancedEntityTestTask extends AbstractEnhancerTestTask {

    private long entityId;

    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Person.class, PersonAddress.class};
    }

    public void prepare() {
        Configuration cfg = new Configuration();
        cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        super.prepare( cfg );

        try ( Session s = getFactory().openSession() ) {
            s.beginTransaction();

            s.persist( new Person( 1L, "Sam" ) );
            s.getTransaction().commit();
        }
    }

    public void execute() {
        try ( Session s = getFactory().openSession() ) {
            s.beginTransaction();
            Person entity = s.find( Person.class, 1L );
            entity.name = "Jhon";
            try {
                s.refresh( entity );
                s.getTransaction().commit();
            } catch ( RuntimeException e ) {
                Assert.fail( "Enhanced entity can't be refreshed: " + e.getMessage() );
            }
        }
    }

    protected void cleanup() {
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        private Long id;

        @Column( name = "name", length = 10, nullable = false )
        private String name;

        @OneToMany( fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true, cascade = CascadeType.ALL )
        private List<PersonAddress> details = new ArrayList<>();

        protected Person() {
        }

        public Person(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Entity(name = "PersonAddress")
    public static class PersonAddress {

        @Id
        private Long id;

        @ManyToOne( optional = false, fetch = FetchType.LAZY )
        private Person parent;
    }
}
