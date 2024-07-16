package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = UpsertVersionedTest.Record.class)
public class UpsertVersionedTest {
    @Test void test(SessionFactoryScope scope) {
        scope.inStatelessTransaction(s-> {
            s.upsert(new Record(123L,null,"hello earth"));
            s.upsert(new Record(456L,2L,"hello mars"));
        });
        scope.inStatelessTransaction(s-> {
            assertEquals("hello earth",s.get(Record.class,123L).message);
            assertEquals("hello mars",s.get(Record.class,456L).message);
        });
        scope.inStatelessTransaction(s-> {
            s.upsert(new Record(123L,0L,"goodbye earth"));
        });
        scope.inStatelessTransaction(s-> {
            assertEquals("goodbye earth",s.get(Record.class,123L).message);
            assertEquals("hello mars",s.get(Record.class,456L).message);
        });
        scope.inStatelessTransaction(s-> {
            s.upsert(new Record(456L,3L,"goodbye mars"));
        });
        scope.inStatelessTransaction(s-> {
            assertEquals("goodbye earth",s.get(Record.class,123L).message);
            assertEquals("goodbye mars",s.get(Record.class,456L).message);
        });
    }
    @Entity(name = "Record")
    static class Record {
        @Id Long id;
        @Version Long version;
        String message;

        Record(Long id, Long version, String message) {
            this.id = id;
            this.version = version;
            this.message = message;
        }

        Record() {
        }
    }
}
