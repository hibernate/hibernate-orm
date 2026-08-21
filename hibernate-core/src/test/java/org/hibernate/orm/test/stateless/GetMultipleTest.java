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

import java.util.List;

import static org.hibernate.LockMode.PESSIMISTIC_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory
@DomainModel(annotatedClasses = GetMultipleTest.Record.class)
public class GetMultipleTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inStatelessTransaction(s-> {
			s.insert(new Record(123L,"hello earth"));
			s.insert(new Record(456L,"hello mars"));
		});
		scope.inStatelessTransaction(s-> {
			List<Record> all = s.getMultiple(Record.class, List.of(456L, 123L, 2L));
			assertEquals("hello mars",all.get(0).message);
			assertEquals("hello earth",all.get(1).message);
			assertNull(all.get(2));
		});
		scope.inStatelessTransaction(s-> {
			List<Record> all = s.getMultiple(Record.class, List.of(123L, 2L, 456L));
			assertEquals("hello earth",all.get(0).message);
			assertEquals("hello mars",all.get(2).message);
			assertNull(all.get(1));
		});
		scope.inStatelessTransaction(s-> {
			List<Record> all = s.getMultiple(Record.class, List.of(456L, 123L, 2L), PESSIMISTIC_READ);
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
