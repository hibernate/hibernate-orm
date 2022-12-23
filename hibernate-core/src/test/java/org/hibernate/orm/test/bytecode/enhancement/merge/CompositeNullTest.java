/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.merge;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions(lazyLoading = true, inlineDirtyChecking = true)
public class CompositeNullTest extends BaseCoreFunctionalTestCase {

    private long entityId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{ParentEntity.class, Address.class};
    }

    @Before
    public void prepare() {
        ParentEntity parent = new ParentEntity();
        parent.description = "Test";

        doInHibernate( this::sessionFactory, s -> {
            s.persist( parent );
        } );

        entityId = parent.id;
    }

    @Test
    @TestForIssue( jiraKey = "HHH-15730")
    public void testNullComposite() {
        doInHibernate( this::sessionFactory, s -> {
            ParentEntity parentEntity = s.find( ParentEntity.class, entityId );
            Assert.assertNull( parentEntity.address );
        } );
    }

    // --- //

    @Entity(name = "Parent")
    @Table( name = "PARENT_ENTITY" )
    private static class ParentEntity {

        @Id
        @GeneratedValue
        Long id;

        String description;

        @Embedded
        Address address;
    }

    @Embeddable
    @Table( name = "ADDRESS" )
    private static class Address {

        String street;

    }
}
