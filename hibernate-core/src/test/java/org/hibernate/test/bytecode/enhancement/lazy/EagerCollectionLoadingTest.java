/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-10186" )
@RunWith( BytecodeEnhancerRunner.class )
public class EagerCollectionLoadingTest extends BaseCoreFunctionalTestCase {
	private static final int CHILDREN_SIZE = 10;
	private Long parentID;
    private Parent parent;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Parent.class, Child.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            Parent parent = new Parent();
            parent.setChildren( new ArrayList<>() );
            for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
                Child child = new Child();
                child.parent = parent;
                s.persist( child );
            }
            s.persist( parent );
            parentID = parent.id;
        } );
    }

	@Test
	public void testTransaction() {
        doInHibernate( this::sessionFactory, s -> {
    		Parent parent = s.load( Parent.class, parentID );
    		assertThat( parent, notNullValue() );
            assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
            assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
    		assertTrue( isPropertyInitialized( parent, "children" ) );

    		List children1 = parent.children;
    		List children2 = parent.children;

    		checkDirtyTracking( parent );

    		assertThat( children1, sameInstance( children2 ) );
    		assertThat( children1.size(), equalTo( CHILDREN_SIZE ) );
        } );
	}

	@Test
	public void testNoTransaction() {
        doInHibernate( this::sessionFactory, s -> {
    		parent = s.load( Parent.class, parentID );
    		assertThat( parent, notNullValue() );
            assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
            assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
    		assertTrue( isPropertyInitialized( parent, "children" ) );
        } );

    		List children1 = parent.children;
    		List children2 = parent.children;

    		checkDirtyTracking( parent );

    		assertThat( children1, sameInstance( children2 ) );
    		assertThat( children1.size(), equalTo( CHILDREN_SIZE ) );
	}

    // --- //

    @Entity
    @Table( name = "PARENT" )
    private static class Parent {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @OneToMany( mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER )
        List<Child> children;

        void setChildren(List<Child> children) {
            this.children = children;
        }
    }

    @Entity
    @Table( name = "CHILD" )
    private static class Child {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @ManyToOne( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
        @LazyToOne( LazyToOneOption.NO_PROXY )
        Parent parent;

        String name;

        Child() {
        }

        Child(String name) {
            this.name = name;
        }
    }
}
