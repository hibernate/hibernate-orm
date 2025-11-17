/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.partition;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@RequiresDialect(SQLServerDialect.class)
@Jpa(annotatedClasses = SQLServerPartitionedTableTest.Partitioned.class,
		integrationSettings = {
				@Setting(name = SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCRIPT_SOURCE,
						value = "org/hibernate/orm/test/sql/partition/sqlserverpartitions-create.sql"),
				@Setting(name = SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SOURCE,
						value = "script-then-metadata"),
				@Setting(name = SchemaToolingSettings.JAKARTA_HBM2DDL_DROP_SCRIPT_SOURCE,
						value = "org/hibernate/orm/test/sql/partition/sqlserverpartitions-drop.sql"),
				@Setting(name = SchemaToolingSettings.JAKARTA_HBM2DDL_DROP_SOURCE,
						value = "metadata-then-script")})
class SQLServerPartitionedTableTest {
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
	@Table(name = "msparts",
		options = "on partScheme(pid)")
	static class Partitioned {
		@Id Long id;
		@PartitionKey Long pid;
		String text = "";
	}
}
