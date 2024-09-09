package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = GetAllTest.Record.class)
public class GetAllTest {
    @Test void test(SessionFactoryScope scope) {
        scope.inStatelessTransaction(s-> {
            s.insert(new Record(123L,"hello earth"));
            s.insert(new Record(456L,"hello mars"));
        });
        scope.inStatelessTransaction(s-> {
            List<Record> all = s.getAll(Record.class, List.of(456L, 123L, 2L));
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
