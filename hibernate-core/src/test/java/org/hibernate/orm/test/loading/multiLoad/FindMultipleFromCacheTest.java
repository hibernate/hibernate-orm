/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.LockMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = FindMultipleFromCacheTest.Record.class)
public class FindMultipleFromCacheTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inStatelessTransaction(s-> {
			s.insert(new Record(123L,"hello earth"));
			s.insert(new Record(456L,"hello mars"));
		});
		scope.inTransaction(s-> {
			List<Record> all = s.findMultiple(Record.class, List.of(456L, 123L, 2L));
			Record mars = all.get( 0 );
			Record earth = all.get( 1 );
			assertEquals( LockMode.READ, s.getCurrentLockMode( mars ) );
			assertEquals( LockMode.READ, s.getCurrentLockMode( earth ) );
			assertEquals("hello mars", mars.message);
			assertEquals("hello earth", earth.message);
			assertNull(all.get(2));
		});
		assertEquals( 0,
				scope.getSessionFactory().getStatistics().getSecondLevelCacheHitCount() );
		scope.getSessionFactory().getStatistics().clear();
		scope.inTransaction(s-> {
			List<Record> all = s.findMultiple(Record.class, List.of(123L, 2L, 456L));
			Record earth = all.get( 0 );
			Record mars = all.get( 2 );
			assertEquals( LockMode.NONE, s.getCurrentLockMode( mars ) );
			assertEquals( LockMode.NONE, s.getCurrentLockMode( earth ) );
			assertEquals("hello earth", earth.message);
			assertEquals("hello mars", mars.message);
			assertNull(all.get(1));
		});
		assertEquals( 2,
				scope.getSessionFactory().getStatistics().getSecondLevelCacheHitCount() );
	}
	@Entity @Cacheable
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
