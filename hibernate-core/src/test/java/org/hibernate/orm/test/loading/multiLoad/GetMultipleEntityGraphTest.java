/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {GetMultipleEntityGraphTest.Record.class, GetMultipleEntityGraphTest.Owner.class})
public class GetMultipleEntityGraphTest {
	@Test void test(SessionFactoryScope scope) {
		var graph = scope.getSessionFactory().createEntityGraph(Record.class);
		graph.addAttributeNode("owner");
		scope.inStatelessTransaction(s-> {
			Owner gavin = new Owner("gavin");
			s.insert(gavin);
			s.insert(new Record(123L,gavin,"hello earth"));
			s.insert(new Record(456L,gavin,"hello mars"));
		});
		scope.inStatelessTransaction(s-> {
			List<Record> all = s.getMultiple(Record.class, List.of(456L, 123L, 2L));
			assertEquals("hello mars",all.get(0).message);
			assertEquals("hello earth",all.get(1).message);
			assertNull(all.get(2));
			assertFalse(Hibernate.isInitialized(all.get(0).owner));
			assertFalse(Hibernate.isInitialized(all.get(1).owner));
		});
		scope.inStatelessTransaction(s-> {
			List<Record> all = s.getMultiple(graph, List.of(456L, 123L));
			assertEquals("hello mars",all.get(0).message);
			assertEquals("hello earth",all.get(1).message);
			assertTrue(Hibernate.isInitialized(all.get(0).owner));
			assertTrue(Hibernate.isInitialized(all.get(1).owner));
		});
	}
	@Entity
	static class Record {
		@Id Long id;
		String message;

		@ManyToOne(fetch = FetchType.LAZY)
		Owner owner;

		Record(Long id, Owner owner, String message) {
			this.id = id;
			this.owner = owner;
			this.message = message;
		}

		Record() {
		}
	}
	@Entity
	static class Owner {
		@Id String name;

		Owner(String name) {
			this.name = name;
		}

		Owner() {
		}
	}
}
