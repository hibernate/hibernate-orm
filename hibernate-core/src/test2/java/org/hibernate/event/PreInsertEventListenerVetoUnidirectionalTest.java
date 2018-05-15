/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.TransientObjectException;
import org.hibernate.action.internal.EntityActionVetoException;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreInsertEventListener;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@TestForIssue( jiraKey = "HHH-11721" )
public class PreInsertEventListenerVetoUnidirectionalTest extends BaseCoreFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Child.class, Parent.class };
    }

    @Override
    protected void afterSessionFactoryBuilt() {
        super.afterSessionFactoryBuilt();
        EventListenerRegistry registry = sessionFactory().getServiceRegistry().getService( EventListenerRegistry.class );
        registry.appendListeners(
                EventType.PRE_INSERT,
                (PreInsertEventListener) event -> event.getEntity() instanceof Parent
        );
    }

    @Test(expected = EntityActionVetoException.class)
    public void testVeto() {
        doInHibernate( this::sessionFactory, session -> {
            Parent parent = new Parent();
            parent.setField1( "f1" );
            parent.setfield2( "f2" );

            Child child = new Child();
            child.setParent( parent );

            session.save( child );
        } );

        fail( "Should have thrown EntityActionVetoException!" );
    }

    @Entity(name = "Child")
    public static class Child {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @OneToOne(cascade = CascadeType.ALL)
        private Parent parent;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }
    }

    @Entity(name = "Parent")
    public static class Parent {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;
        private String field1;
        private String field2;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setfield2(String field2) {
            this.field2 = field2;
        }
    }
}
