/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.StaleObjectStateException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = UpsertVersionedTest.Record.class)
public class UpsertVersionedTest {

	@Test void test(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inStatelessTransaction(s-> {
			s.upsert(new Record(123L,null,"hello earth"));
			s.upsert(new Record(456L,2L,"hello mars"));
		});
		scope.inStatelessTransaction(s-> {
			assertEquals( "hello earth", s.get( Record.class,123L).message );
			assertEquals( "hello mars", s.get( Record.class,456L).message );
		});
		scope.inStatelessTransaction(s-> {
			s.upsert(new Record(123L,0L,"goodbye earth"));
		});
		scope.inStatelessTransaction(s-> {
			assertEquals( "goodbye earth", s.get( Record.class,123L).message );
			assertEquals( "hello mars", s.get( Record.class,456L).message );
		});
		scope.inStatelessTransaction(s-> {
			s.upsert(new Record(456L,3L,"goodbye mars"));
		});
		scope.inStatelessTransaction(s-> {
			assertEquals( "goodbye earth", s.get( Record.class,123L).message );
			assertEquals( "goodbye mars", s.get( Record.class,456L).message );
		});
	}

	@Test void testStaleUpsert(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inStatelessTransaction( s -> {
			s.insert(new Record(789L, 1L, "hello world"));
		} );
		scope.inStatelessTransaction( s -> {
			s.upsert(new Record(789L, 1L, "hello mars"));
		} );
		try {
			scope.inStatelessTransaction( s -> {
				s.upsert(new Record( 789L, 1L, "hello venus"));
			} );
			fail();
		}
		catch (StaleObjectStateException sose) {
			//expected
		}
		scope.inStatelessTransaction( s-> {
			assertEquals( "hello mars", s.get(Record.class,789L).message );
		} );
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
