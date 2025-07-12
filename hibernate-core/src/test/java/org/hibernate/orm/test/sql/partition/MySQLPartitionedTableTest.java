/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.partition;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiresDialect(MySQLDialect.class)
@Jpa(annotatedClasses = MySQLPartitionedTableTest.Partitioned.class)
class MySQLPartitionedTableTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			Partitioned partitioned = new Partitioned();
			partitioned.id = 1L;
			partitioned.pid = 500L;
			session.persist( partitioned );
		} );
		scope.inTransaction( session -> {
			Partitioned partitioned = session.find( Partitioned.class, 1L );
			assertNotNull( partitioned );
			partitioned.text = "updated";
		} );
	}
	@Entity
	@Table(name = "myparts",
		options =
			"""
			PARTITION BY RANGE (pid) (
				PARTITION p1 VALUES LESS THAN (1000),
				PARTITION p2 VALUES LESS THAN (2000)
			)
			""")
	static class Partitioned {
		@Id Long id;
		@PartitionKey Long pid;
		String text = "";
	}
}
