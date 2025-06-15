package org.hibernate.orm.test.records;

import jakarta.persistence.*;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-19542")
@DomainModel(
        annotatedClasses = {
                RecordEmbeddableAsSecondaryTableTest.User.class
        })
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true, extendedEnhancement = true, inlineDirtyChecking = true)
public class RecordEmbeddableAsSecondaryTableTest {

    @BeforeAll
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction(session -> {
            Person person = new Person(new FullName("Sylvain", "Lecoy"), 38);

            User user = new User(1, person);

            session.persist( user );
        });
    }

    @Test
    public void testGetEntityA(SessionFactoryScope scope) {
        scope.inTransaction(session -> {
            User user = session.get(User.class, 1);
            assertThat( user ).isNotNull();
        });
    }

    @Entity
    @SecondaryTable(name = "Person")
    public static class User {

        @Id
        private Integer id;

        private Person person;

        public User(
                final Integer id,
                final Person person) {
            this.id = id;
            this.person = person;
        }

        protected User() {

        }
    }

    @Embeddable
    public record Person( // If we invert, and put age as first argument, the test passes.
            FullName fullName,
            @Column(table = "Person")
            Integer age) {
    }

    @Embeddable
    public record FullName(
            String firstName,
            String lastName) {
    }
}
