/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.cascade;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-10252" )
@DomainModel(
        annotatedClasses = {
               CascadeDeleteCollectionTest.Parent.class, CascadeDeleteCollectionTest.Child.class
        }
)
@SessionFactory
@BytecodeEnhanced
public class CascadeDeleteCollectionTest {
    private Parent originalParent;


    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        // Create a Parent with one Child
        originalParent = scope.fromTransaction( s -> {
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
    public void testManagedWithUninitializedAssociation(SessionFactoryScope scope) {
        // Delete the Parent
        scope.inTransaction( s -> {
            Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
                    .setParameter( "name", "PARENT" )
                    .uniqueResult();
            checkInterceptor( scope, loadedParent, false );
            assertFalse( Hibernate.isInitialized( loadedParent.getChildren() ) );
            s.delete( loadedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @JiraKey("HHH-13129")
    public void testManagedWithInitializedAssociation(SessionFactoryScope scope) {
        // Delete the Parent
        scope.inTransaction( s -> {
            Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
                    .setParameter( "name", "PARENT" )
                    .uniqueResult();
            checkInterceptor( scope, loadedParent, false );
            loadedParent.getChildren().size();
            assertTrue( Hibernate.isInitialized( loadedParent.getChildren() ) );
            s.delete( loadedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @JiraKey("HHH-13129")
    public void testDetachedWithUninitializedAssociation(SessionFactoryScope scope) {
        final Parent detachedParent = scope.fromTransaction( s -> {
            return s.get( Parent.class, originalParent.getId() );
        } );

        assertFalse( Hibernate.isInitialized( detachedParent.getChildren() ) );

        checkInterceptor( scope, detachedParent, false );

        // Delete the detached Parent with uninitialized children
        scope.inTransaction( s -> {
             s.delete( detachedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @JiraKey("HHH-13129")
    public void testDetachedWithInitializedAssociation(SessionFactoryScope scope) {
        final Parent detachedParent = scope.fromTransaction( s -> {
             Parent parent = s.get( Parent.class, originalParent.getId() );
             // initialize collection before detaching
             parent.getChildren().size();
             return parent;
        } );

        assertTrue( Hibernate.isInitialized( detachedParent.getChildren() ) );

        checkInterceptor( scope, detachedParent, false );

        // Delete the detached Parent with initialized children
        scope.inTransaction( s -> {
            s.delete( detachedParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    @Test
    @JiraKey("HHH-13129")
    public void testDetachedOriginal(SessionFactoryScope scope) {

        // originalParent#children should be initialized
        assertTrue( Hibernate.isPropertyInitialized( originalParent, "children" ) );

        checkInterceptor( scope, originalParent, true );

        // Delete the Parent
        scope.inTransaction( s -> {
            s.delete( originalParent );
        } );
        // If the lazy relation is not fetch on cascade there is a constraint violation on commit
    }

    private void checkInterceptor(SessionFactoryScope scope, Parent parent, boolean isNullExpected) {
        final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = scope.getSessionFactory().getRuntimeMetamodels()
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
            return children;
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
    static class Child {

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
