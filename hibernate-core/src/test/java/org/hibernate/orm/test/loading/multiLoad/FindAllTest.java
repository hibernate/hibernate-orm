package org.hibernate.orm.test.loading.multiLoad;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = FindAllTest.Record.class)
public class FindAllTest {
    @Test void test(SessionFactoryScope scope) {
        scope.inTransaction(s-> {
            s.persist(new Record(123L,"hello earth"));
            s.persist(new Record(456L,"hello mars"));
        });
        scope.inTransaction(s-> {
            List<Record> all = s.findAll(Record.class, List.of(456L, 123L, 2L));
            assertEquals("hello mars",all.get(0).message);
            assertEquals("hello earth",all.get(1).message);
            assertNull(all.get(2));
        });
    }
    @Entity
    static class Record {
        @Id Long id;
        String message;

        Record(Long id, String message) {
            this.id = id;
            this.message = message;
        }

        Record() {
        }
    }
}
