package org.hibernate.test.schemaupdate.index;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-11913")
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQL81Dialect.class)
@RequiresDialect(MySQLDialect.class)
public class IndexesCreationTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private Metadata metadata;

	@Before
	public void setUp() {
		ssr = new StandardServiceRegistryBuilder().build();
		metadata = new MetadataSources( ssr )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
	}

	@Test
	public void testTheIndexIsGenerated() {
		final List<String> commands = new SchemaCreatorImpl( ssr ).generateCreationCommands(
				metadata,
				false
		);

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
			}
		}
		assertTrue(

				"Expected " + expectedCommand + " command not found",
				createIndexCommandIsGenerated
		);
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
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
