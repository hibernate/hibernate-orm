/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.partition;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiresDialect(DB2Dialect.class)
@Jpa(annotatedClasses = Db2PartitionedTableTest.Partitioned.class)
class Db2PartitionedTableTest {
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
	@Table(name = "db2parts",
		options =
			"""
			PARTITION BY RANGE (pid) (
				STARTING FROM (0) ENDING AT (1000),
				ENDING AT (2000)
			)
			""")
	static class Partitioned {
		@Id Long id;
		@PartitionKey Long pid;
		String text = "";
	}
}
