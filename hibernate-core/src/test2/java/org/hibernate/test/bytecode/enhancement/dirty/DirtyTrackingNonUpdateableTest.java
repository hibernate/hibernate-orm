/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.dirty;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-12051" )
@RunWith( BytecodeEnhancerRunner.class )
public class DirtyTrackingNonUpdateableTest extends BaseCoreFunctionalTestCase {

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Thing.class};
    }

    @Test
    public void test() {
        doInJPA( this::sessionFactory, entityManager -> {
            Thing thing = new Thing();
            entityManager.persist( thing );

            entityManager
            .createQuery( "update thing set special = :s, version = version + 1" )
            .setParameter( "s", "new" )
            .executeUpdate();

            thing.special = "If I'm flush to the DB you get an OptimisticLockException";
        } );
    }

    // --- //

    @Entity( name = "thing" )
    @Table( name = "THING_ENTITY" )
    public class Thing {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        long id;

        @Version
        long version;

        @Column( updatable = false )
        String special;
    }
}
