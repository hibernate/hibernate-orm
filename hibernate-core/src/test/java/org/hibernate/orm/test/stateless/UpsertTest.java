/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = UpsertTest.Record.class)
public class UpsertTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inStatelessTransaction(s-> {
			s.upsert(new Record(123L,"hello earth"));
			s.upsert(new Record(456L,"hello mars"));
		});
		scope.inStatelessTransaction(s-> {
			assertEquals("hello earth", s.get( Record.class,123L).message);
			assertEquals("hello mars", s.get( Record.class,456L).message);
		});
		scope.inStatelessTransaction(s-> {
			s.upsert(new Record(123L,"goodbye earth"));
		});
		scope.inStatelessTransaction(s-> {
			assertEquals("goodbye earth", s.get( Record.class,123L).message);
			assertEquals("hello mars", s.get( Record.class,456L).message);
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
