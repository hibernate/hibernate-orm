package org.hibernate.orm.test.collection;

import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@DomainModel(annotatedClasses = {
        OneToManyUnidirectionalBatchingTest.Person.class,
        OneToManyUnidirectionalBatchingTest.Phone.class,
})
@SessionFactory
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "50"),
        }
)
@JiraKey(value = "HHH-19322")
public class OneToManyUnidirectionalBatchingTest {

    private final AtomicReference<Long> person1Id = new AtomicReference<>();
    private final AtomicReference<Long> person2Id = new AtomicReference<>();
    private final AtomicReference<Long> phoneId = new AtomicReference<>();

    @BeforeAll
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction(session -> {
            Phone phone = new Phone();
            Phone dontCare = new Phone();
            session.persist(phone);
            phoneId.set(phone.id);
            session.persist(dontCare);

            Person person1 = new Person();
            Person person2 = new Person();
            session.persist(person1);
            person1Id.set(person1.id);
            session.persist(person2);
            person2Id.set(person2.id);

            person1.getPhones().addAll(Arrays.asList(phone, dontCare));
        });
    }

    @Test
    public void testBatching(SessionFactoryScope scope) {
        scope.inTransaction(session -> {
            // load person2 first, such that changes to its phones collection are flushed before person1's
            Person person2 = session.find(Person.class, person2Id.get());
            Person person1 = session.find(Person.class, person1Id.get());
            Phone phone = session.find(Phone.class, phoneId.get());

            person1.getPhones().remove(phone);
            person2.getPhones().add(phone);
        });
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        @GeneratedValue
        private Long id;

        @JoinColumn(name = "person_id")
        @OneToMany
        private List<Phone> phones = new ArrayList<>();

        public List<Phone> getPhones() {
            return phones;
        }
    }

    @Entity(name = "Phone")
    public static class Phone {

        @Id
        @GeneratedValue
        private Long id;
    }

}
