/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderService;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Same as {@link CascadeDeleteCollectionTest},
 * but with {@code collectionInDefaultFetchGroup} set to {@code false} explicitly.
 * <p>
 * Kept here for <a href="https://github.com/hibernate/hibernate-orm/pull/5252#pullrequestreview-1095843220">historical reasons</a>.
 *
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-10252" )
@RunWith( BytecodeEnhancerRunner.class )
public class CascadeDeleteCollectionWithCollectionInDefaultFetchGroupFalseTest extends BaseCoreFunctionalTestCase {
    private Parent originalParent;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{Parent.class, Child.class};
    }

    @Override
    protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
        serviceRegistryBuilder.addService(
                SessionFactoryBuilderService.class,
                (SessionFactoryBuilderService) (metadata, bootstrapContext) -> {
                    SessionFactoryOptionsBuilder optionsBuilder = new SessionFactoryOptionsBuilder(
                            metadata.getMetadataBuildingOptions().getServiceRegistry(),
                            bootstrapContext
                    );
                    // We want to test with this setting set to false explicitly,
                    // because another test already takes care of the default.
                    optionsBuilder.enableCollectionInDefaultFetchGroup( false );
                    return new SessionFactoryBuilderImpl( metadata, optionsBuilder );
                }
        );
    }

    @Before
    public void prepare() {
        // Create a Parent with one Child
        originalParent = doInHibernate( this::sessionFactory, s -> {
                    Parent p = new Parent();
                    p.setName( "PARENT" );
                    p.setLazy( "LAZY" );
                    p.makeChild();
                    s.persist( p );
                    return p;
                }
        );
    }

    @Test
    public void testManagedWithUninitializedAssociation() {
        // Delete the Parent
        doInHibernate( this::sessionFactory, s -> {
            Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
                    .setParameter( "name", "PARENT" )
                    .uniqueResult();
            checkInterceptor( loadedParent, false );
            assertFalse( Hibernate.isPropertyInitialized( loadedParent, "children" ) );
            s.delete( loadedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @TestForIssue(jiraKey = "HHH-13129")
    public void testManagedWithInitializedAssociation() {
        // Delete the Parent
        doInHibernate( this::sessionFactory, s -> {
            Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
                    .setParameter( "name", "PARENT" )
                    .uniqueResult();
            checkInterceptor( loadedParent, false );
            loadedParent.getChildren();
            assertTrue( Hibernate.isPropertyInitialized( loadedParent, "children" ) );
            s.delete( loadedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @TestForIssue(jiraKey = "HHH-13129")
    public void testDetachedWithUninitializedAssociation() {
        final Parent detachedParent = doInHibernate( this::sessionFactory, s -> {
            return s.get( Parent.class, originalParent.getId() );
        } );

        assertFalse( Hibernate.isPropertyInitialized( detachedParent, "children" ) );

        checkInterceptor( detachedParent, false );

        // Delete the detached Parent with uninitialized children
        doInHibernate( this::sessionFactory, s -> {
             s.delete( detachedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @TestForIssue(jiraKey = "HHH-13129")
    public void testDetachedWithInitializedAssociation() {
        final Parent detachedParent = doInHibernate( this::sessionFactory, s -> {
             Parent parent = s.get( Parent.class, originalParent.getId() );
             assertFalse( Hibernate.isPropertyInitialized( parent, "children" ) );

             // initialize collection before detaching
             parent.getChildren();
             return parent;
        } );

        assertTrue( Hibernate.isPropertyInitialized( detachedParent, "children" ) );

        checkInterceptor( detachedParent, false );

        // Delete the detached Parent with initialized children
        doInHibernate( this::sessionFactory, s -> {
            s.delete( detachedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @TestForIssue(jiraKey = "HHH-13129")
    public void testDetachedOriginal() {

        // originalParent#children should be initialized
        assertTrue( Hibernate.isPropertyInitialized( originalParent, "children" ) );

        checkInterceptor( originalParent, true );

        // Delete the Parent
        doInHibernate( this::sessionFactory, s -> {
            s.delete( originalParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    private void checkInterceptor(Parent parent, boolean isNullExpected) {
        final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = sessionFactory().getRuntimeMetamodels()
                .getMappingMetamodel()
                .getEntityDescriptor( Parent.class )
                .getBytecodeEnhancementMetadata();
        if ( isNullExpected ) {
            // if a null Interceptor is expected, then there shouldn't be any uninitialized attributes
            assertFalse( bytecodeEnhancementMetadata.hasUnFetchedAttributes( parent ) );
            assertNull( bytecodeEnhancementMetadata.extractInterceptor( parent ) );
        }
        else {
            assertNotNull( bytecodeEnhancementMetadata.extractInterceptor( parent ) );
        }
    }

    // --- //

    @Entity( name = "Parent" )
    @Table( name = "PARENT" )
    public static class Parent {

        Long id;

        String name;

        List<Child> children = new ArrayList<>();

        String lazy;

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }

        @OneToMany( mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.LAZY )
        List<Child> getChildren() {
            return Collections.unmodifiableList( children );
        }

        void setChildren(List<Child> children) {
            this.children = children;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        @Basic( fetch = FetchType.LAZY )
        String getLazy() {
            return lazy;
        }

        void setLazy(String lazy) {
            this.lazy = lazy;
        }

        void makeChild() {
            Child c = new Child();
            c.setParent( this );
            children.add( c );
        }
    }

    @Entity
    @Table( name = "CHILD" )
    private static class Child {

        @Id
        @GeneratedValue( strategy = GenerationType.AUTO )
        Long id;

        @ManyToOne( optional = false )
        @JoinColumn( name = "parent_id" )
        Parent parent;

        Long getId() {
            return id;
        }

        void setId(Long id) {
            this.id = id;
        }

        Parent getParent() {
            return parent;
        }

        void setParent(Parent parent) {
            this.parent = parent;
        }
    }
}
