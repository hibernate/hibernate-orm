/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.index;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-11913")
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(MySQLDialect.class)
@ServiceRegistry
@DomainModel(annotatedClasses = IndexesCreationTest.TestEntity.class)
public class IndexesCreationTest {
	@Test
	public void testTheIndexIsGenerated(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		final List<String> commands = new SchemaCreatorImpl( registryScope.getRegistry() )
				.generateCreationCommands( modelScope.getDomainModel(), false );

		assertThatCreateIndexCommandIsGenerated( "CREATE INDEX FIELD_1_INDEX ON TEST_ENTITY (FIELD_1)", commands );
		assertThatCreateIndexCommandIsGenerated(
				"CREATE INDEX FIELD_2_INDEX ON TEST_ENTITY (FIELD_2 DESC, FIELD_3 ASC)",
				commands
		);
		assertThatCreateIndexCommandIsGenerated(
				"CREATE INDEX FIELD_4_INDEX ON TEST_ENTITY (FIELD_4 ASC)",
				commands
		);
	}

	private void assertThatCreateIndexCommandIsGenerated(String expectedCommand, List<String> commands) {
		boolean createIndexCommandIsGenerated = false;
		for ( String command : commands ) {
			if ( command.toLowerCase().contains( expectedCommand.toLowerCase() ) ) {
				createIndexCommandIsGenerated = true;
				break;
			}
		}
		Assertions.assertTrue( createIndexCommandIsGenerated, "Expected " + expectedCommand + " command not found" );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY",
			indexes = {
					@Index(name = "FIELD_1_INDEX", columnList = "FIELD_1"),
					@Index(name = "FIELD_2_INDEX", columnList = "FIELD_2 DESC, FIELD_3 ASC"),
					@Index(name = "FIELD_4_INDEX", columnList = "FIELD_4 ASC")
			}
	)
	public static class TestEntity {
		private long id;
		private String field1;
		private String field2;
		private String field3;
		private String field4;

		@Id
		@Column
		public long getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Column(name = "FIELD_1")
		public String getField1() {
			return field1;
		}

		public void setField1(String field1) {
			this.field1 = field1;
		}

		@Column(name = "FIELD_2")
		public String getField2() {
			return field2;
		}

		public void setField2(String field2) {
			this.field2 = field2;
		}

		@Column(name = "FIELD_3")
		public String getField3() {
			return field3;
		}

		public void setField3(String field3) {
			this.field3 = field3;
		}

		@Column(name = "FIELD_4")
		public String getField4() {
			return field4;
		}

		public void setField4(String field4) {
			this.field4 = field4;
		}
	}
}
