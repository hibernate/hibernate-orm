package org.hibernate.orm.test.mapping.onetoone.pkjoincolumn;

import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = {OneToOneConstraintTest.Person.class, OneToOneConstraintTest.User.class})
public class OneToOneConstraintTest {

    @Test
    void test(SessionFactoryScope scope) {
        try {
            scope.inTransaction(s -> s.persist(new Person()));
            fail();
        }
        catch (ConstraintViolationException cve) {
        }
    }

    @Entity(name="persons")
    static class Person {
        @Id @GeneratedValue
        long pid;
        // note missing @MapsId and optional=false
        // test that the FK gets created anyway
        @OneToOne
        @PrimaryKeyJoinColumn(name = "pid")
        private User user;
    }

    @Entity(name="users")
    static class User {
        @Id @GeneratedValue
        long uid;
    }
}
