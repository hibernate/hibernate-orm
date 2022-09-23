package org.hibernate.orm.test.deleteunloaded;

import org.hibernate.Transaction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel( annotatedClasses = { Parent.class, Child.class } )
@SessionFactory
//@ServiceRegistry(
//        settings = {
//                @Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
//        }
//)
public class DeleteUnloadedProxyTest {
    @Test
    public void testAttached(SessionFactoryScope scope) {
        Parent p = new Parent();
        Child c = new Child();
        scope.inSession( em -> {
            Transaction tx = em.beginTransaction();
            c.setParent(p);
            p.getChildren().add(c);
            em.persist(p);
            tx.commit();
        } );
        scope.inSession( em -> {
            Transaction tx = em.beginTransaction();
            Child child = em.getReference( Child.class, c.getId() );
            assertFalse( isInitialized(child) );
            em.remove(child);
            Parent parent = em.getReference( Parent.class, p.getId() );
            assertFalse( isInitialized(parent) );
            em.remove(parent);
            tx.commit();
            assertFalse( isInitialized(child) );
            assertFalse( isInitialized(parent) );
        } );
        scope.inSession( em -> {
            assertNull( em.find( Parent.class, p.getId() ) );
            assertNull( em.find( Child.class, c.getId() ) );
        } );
    }
    @Test
    public void testDetached(SessionFactoryScope scope) {
        Parent p = new Parent();
        Child c = new Child();
        scope.inSession( em -> {
            Transaction tx = em.beginTransaction();
            c.setParent(p);
            p.getChildren().add(c);
            em.persist(p);
            tx.commit();
        } );
        Child cc = scope.fromSession( em -> {
            Transaction tx = em.beginTransaction();
            Child child = em.getReference( Child.class, c.getId() );
            assertFalse( isInitialized(child) );
            return child;
        } );
        Parent pp = scope.fromSession( em -> {
            Transaction tx = em.beginTransaction();
            Parent parent = em.getReference( Parent.class, p.getId() );
            assertFalse( isInitialized(parent) );
            return parent;
        } );
        scope.inSession( em -> {
            Transaction tx = em.beginTransaction();
            em.remove(cc);
            em.remove(pp);
            tx.commit();
            assertFalse( isInitialized(cc) );
            assertFalse( isInitialized(pp) );
        } );
        scope.inSession( em -> {
            assertNull( em.find( Parent.class, p.getId() ) );
            assertNull( em.find( Child.class, c.getId() ) );
        } );
    }
}
