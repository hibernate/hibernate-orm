/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory(useCollectingStatementInspector = true)
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
		scope.inStatelessTransaction(s-> s.upsert(new Record(123L,"goodbye earth")) );
		scope.inStatelessTransaction(s-> {
			assertEquals("goodbye earth", s.get( Record.class,123L).message);
			assertEquals("hello mars", s.get( Record.class,456L).message);
		});
	}

	@RequiresDialects(
			value = {
					@RequiresDialect( MySQLDialect.class ),
					@RequiresDialect( MariaDBDialect.class )
			}
	)
	@Test void testMySQL(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inStatelessTransaction(s-> {
			s.upsert(new Record(123L,"hello earth"));
			s.upsert(new Record(456L,"hello mars"));
		});
		// Verify that only a single query is executed for each upsert, in contrast to the former update+insert
		statementInspector.assertExecutedCount( 2 );

		scope.inStatelessTransaction(s-> {
			assertEquals("hello earth",s.get(Record.class,123L).message);
			assertEquals("hello mars",s.get(Record.class,456L).message);
		});
		statementInspector.clear();

		scope.inStatelessTransaction(s-> s.upsert(new Record(123L,"goodbye earth")) );
		statementInspector.assertExecutedCount( 1 );

		scope.inStatelessTransaction(s-> {
			assertEquals("goodbye earth",s.get(Record.class,123L).message);
			assertEquals("hello mars",s.get(Record.class,456L).message);
		});
	}

	@RequiresDialects(
			value = {
					@RequiresDialect( MySQLDialect.class ),
					@RequiresDialect( MariaDBDialect.class )
			}
	)
	@Test void testMySQLRowCounts(SessionFactoryScope scope) {
		// insert => rowcount 1
		scope.inStatelessTransaction(s-> assertDoesNotThrow(() -> s.upsert(new Record(123L,"hello earth", 321))) );

		// Partial update => rowcount 2
		scope.inStatelessTransaction(s-> assertDoesNotThrow(() -> s.upsert(new Record(123L,"goodbye earth"))) );

		// Nothing updated => rowcount 1 (?)
		scope.inStatelessTransaction(s-> assertDoesNotThrow(() -> s.upsert(new Record(123L,"goodbye earth"))) );

		// all null => delete
		scope.inStatelessTransaction(s-> assertDoesNotThrow(() -> s.upsert(new Record(123L,null, null))) );
	}

	@Entity
	static class Record {
		@Id Long id;
		String message;
		Integer someInt;

		Record(Long id, String message) {
			this.id = id;
			this.message = message;
		}

		Record(Long id, String message, Integer someInt) {
			this.id = id;
			this.message = message;
			this.someInt = someInt;
		}

		Record() {
		}
	}
}
