package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = {
		UpsertTest.Record.class,
		UpsertTest.IdOnly.class,
		UpsertTest.IdOnlyIntermediate.class,
		UpsertTest.IdOnlySubtype.class
})
public class UpsertTest {
    @Test void test(SessionFactoryScope scope) {
        scope.inStatelessTransaction(s-> {
            s.upsert(new Record(123L,"hello earth"));
            s.upsert(new Record(456L,"hello mars"));
        });
        scope.inStatelessTransaction(s-> {
            assertEquals("hello earth",s.get(Record.class,123L).message);
            assertEquals("hello mars",s.get(Record.class,456L).message);
        });
        scope.inStatelessTransaction(s-> {
            s.upsert(new Record(123L,"goodbye earth"));
        });
        scope.inStatelessTransaction(s-> {
            assertEquals("goodbye earth",s.get(Record.class,123L).message);
            assertEquals("hello mars",s.get(Record.class,456L).message);
        });
    }
    @Test void testIdOnly(SessionFactoryScope scope) {
		scope.inTransaction(s-> {
			s.createMutationQuery( "delete from IdOnly" ).executeUpdate();
		});
		scope.inStatelessTransaction(s-> {
			s.upsert(new IdOnly(123L));
		});
		scope.inStatelessTransaction(s-> {
			assertNotNull(s.get( IdOnly.class,123L));
		});
		scope.inStatelessTransaction(s-> {
			s.upsert(new IdOnly(123L));
		});
		scope.inStatelessTransaction(s-> {
			assertNotNull(s.get( IdOnly.class,123L));
		});
	}

	@Test void testIdOnlySubtype(SessionFactoryScope scope) {
		scope.inTransaction(s-> {
			s.createMutationQuery( "delete from IdOnly" ).executeUpdate();
		});

		scope.inStatelessTransaction(s-> {
			s.upsert(new IdOnlySubtype(123L));
		});
		scope.inStatelessTransaction(s-> {
			assertNotNull(s.get( IdOnlySubtype.class,123L));
		});
		scope.inStatelessTransaction(s-> {
			s.upsert(new IdOnlySubtype(123L));
		});
		scope.inStatelessTransaction(s-> {
			assertNotNull(s.get( IdOnlySubtype.class,123L));
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

	@Entity(name = "IdOnly")
	@Inheritance(strategy = InheritanceType.JOINED)
	static class IdOnly {
		@Id Long id;

		IdOnly(Long id) {
			this.id = id;
		}

		IdOnly() {
		}
	}

	@Entity(name = "IdOnlyIntermediate")
	static class IdOnlyIntermediate extends IdOnly {
		IdOnlyIntermediate(Long id) {
			super( id );
		}

		IdOnlyIntermediate() {
		}
	}

	@Entity(name = "IdOnlySubtype")
	static class IdOnlySubtype extends IdOnlyIntermediate {
		String name;

		IdOnlySubtype(Long id) {
			super( id );
		}

		IdOnlySubtype() {
		}
	}
}
