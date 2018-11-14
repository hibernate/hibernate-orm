package org.hibernate.orm.test.tool.schemacreation.index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

@TestForIssue(jiraKey = "HHH-11913")
@RequiresDialect(dialectClass = H2Dialect.class)
@RequiresDialect(dialectClass = PostgreSQL81Dialect.class)
@RequiresDialect(dialectClass = MySQLDialect.class)
public class IndexesCreationTest extends BaseSchemaCreationTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@SchemaTest
	public void testTheIndexIsGenerated(SchemaScope schemaScope) {
		assertThatActionIsGenerated( "CREATE INDEX FIELD_1_INDEX ON TEST_ENTITY \\(FIELD_1\\)" );
		assertThatActionIsGenerated( "CREATE INDEX FIELD_2_INDEX ON TEST_ENTITY \\(FIELD_2 DESC, FIELD_3 ASC\\)" );
		assertThatActionIsGenerated( "CREATE INDEX FIELD_4_INDEX ON TEST_ENTITY \\(FIELD_4 ASC\\)" );
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
