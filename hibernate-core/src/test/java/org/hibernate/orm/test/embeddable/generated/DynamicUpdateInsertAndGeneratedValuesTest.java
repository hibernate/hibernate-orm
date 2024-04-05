package org.hibernate.orm.test.embeddable.generated;

import java.sql.PreparedStatement;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(
		annotatedClasses = {
				DynamicUpdateInsertAndGeneratedValuesTest.TestEntity.class
		}
)
@SessionFactory(
		exportSchema = false,
		useCollectingStatementInspector = true
)
@Jira("HHH-16957")
public class DynamicUpdateInsertAndGeneratedValuesTest {
	private final static String NON_GENERATED_PROP_0 = "a";
	private final static String UPDATED_NON_GENERATED_PROP_0 = "b";
	private final static int NON_GENERATED_PROP_1 = 20;
	private final static int UPDATED_NON_GENERATED_PROP_1 = 20;

	private final static int ALWAYS_GENERATED_PROP_0 = 1;
	private final static int ALWAYS_GENERATED_PROP_WRITABLE_0 = 2;
	private final static int ALWAYS_GENERATED_PROP_1 = 3;
	private final static int ALWAYS_GENERATED_PROP_WRITABLE_1 = 4;
	private final static int INSERT_GENERATED_PROP_1 = 5;
	private final static int INSERT_GENERATED_PROP_WRITABLE_1 = 6;

	private final static String NON_GENERATED_PROP_0_COLUMN = "non_generate_prop_0";
	private final static String ALWAYS_GENERATED_PROP_0_COLUMN = "always_generated_prop_0";
	private final static String ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN = "always_generated_prop_writable_0";

	private final static String NON_GENERATED_PROP_1_COLUMN = "non_generate_prop_1";
	private final static String ALWAYS_GENERATED_PROP_1_COLUMN = "always_generated_prop_1";
	private final static String ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN = "always_generated_prop_writable_1";
	private final static String INSERT_GENERATED_PROP_1_COLUMN = "insert_generated_prop_1";
	private final static String INSERT_GENERATED_PROP_WRITABLE_1_COLUMN = "insert_generated_prop_writable_1";


	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork( connection -> {
							PreparedStatement createTablePreparedStatement = connection.prepareStatement(
									"create table IF NOT EXISTS test_entity (" +
											" id integer not null, " +
											NON_GENERATED_PROP_0_COLUMN + " varchar(255), " +
											ALWAYS_GENERATED_PROP_0_COLUMN + " integer generated always as ( " + ALWAYS_GENERATED_PROP_0 + " ) stored, " +
											ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN + " integer, " +
											NON_GENERATED_PROP_1_COLUMN + " integer, " +
											ALWAYS_GENERATED_PROP_1_COLUMN + " integer generated always as ( " + ALWAYS_GENERATED_PROP_1 + " ) stored, " +
											ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN + " integer, " +
											INSERT_GENERATED_PROP_1_COLUMN + " integer generated always as ( " + INSERT_GENERATED_PROP_1 + " ) stored, " +
											INSERT_GENERATED_PROP_WRITABLE_1_COLUMN + " integer, " +
											" primary key (id)" +
											" )" );
							try {
								createTablePreparedStatement.execute();
							}
							finally {
								createTablePreparedStatement.close();
							}

							PreparedStatement afterInsertFunctionPreparedStatement = connection.prepareStatement(
									"create or replace function afterInsert() returns trigger as  $afterInsert$ begin " +
											"update test_entity set " +
											ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN + " = " + ALWAYS_GENERATED_PROP_WRITABLE_0 + " , " +
											ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN + " = " + ALWAYS_GENERATED_PROP_WRITABLE_1 + " , " +
											INSERT_GENERATED_PROP_WRITABLE_1_COLUMN + " = " + INSERT_GENERATED_PROP_WRITABLE_1 +
											" where id = NEW.id; return NEW; END; $afterInsert$ LANGUAGE plpgsql" );
							try {
								afterInsertFunctionPreparedStatement.execute();
							}
							finally {
								afterInsertFunctionPreparedStatement.close();
							}

							PreparedStatement afterInsertTriggerPreparedStatement = connection.prepareStatement(
									"create or replace trigger after_insert_t after insert on test_entity for each row execute function afterInsert();" );
							try {
								afterInsertTriggerPreparedStatement.execute();
							}
							finally {
								afterInsertTriggerPreparedStatement.close();
							}
						} )
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork( connection -> {

							PreparedStatement dropAfterInsertTrigger = connection
									.prepareStatement( "drop trigger IF EXISTS after_insert_t on test_entity" );
							try {
								dropAfterInsertTrigger.execute();
							}
							finally {
								dropAfterInsertTrigger.close();
							}

							PreparedStatement dropAfterInsertFunction = connection
									.prepareStatement( "drop function IF EXISTS afterInsert" );
							try {
								dropAfterInsertFunction.execute();
							}
							finally {
								dropAfterInsertFunction.close();
							}

							PreparedStatement dropTable = connection.prepareStatement( "drop table test_entity" );
							try {
								dropTable.execute();
							}
							finally {
								dropTable.close();
							}
						} )
		);
	}

	@Test
	public void testPersistAndFind(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInspector = (SQLStatementInspector) scope.getStatementInspector();
		sqlStatementInspector.clear();

		TestEntity testEntity = new TestEntity(
				1,
				NON_GENERATED_PROP_0,
				new EmbeddableValue( NON_GENERATED_PROP_1 )
		);

		scope.inTransaction(
				session ->
						session.persist( testEntity )
		);

		sqlStatementInspector.assertExecutedCount( 2 );
		sqlStatementInspector.assertIsInsert( 0 );
		sqlStatementInspector.assertIsSelect( 1 );

		assertThatInsertIsCorrect( sqlStatementInspector.getSqlQueries().get( 0 ) );
		assertThatSelectOnInsertIsCorrect(
				sqlStatementInspector.getSqlQueries().get( 1 ),
				testEntity
		);

		sqlStatementInspector.clear();
		scope.inTransaction(
				session -> {
					TestEntity found = session.find( TestEntity.class, 1 );

					sqlStatementInspector.assertExecutedCount( 1 );
					sqlStatementInspector.assertIsSelect( 0 );
					assertThatSelectStatementIsCorrect( sqlStatementInspector.getSqlQueries().get( 0 ) );

					sqlStatementInspector.clear();

					found.setName( UPDATED_NON_GENERATED_PROP_0 );
					found.getEmbeddableValue().setNonGeneratedProperty1( UPDATED_NON_GENERATED_PROP_1 );
					session.flush();

					sqlStatementInspector.assertExecutedCount( 2 );
					sqlStatementInspector.assertIsUpdate( 0 );
					assertThatUpdateStatementIsCorrect( sqlStatementInspector.getSqlQueries().get( 0 ) );
					assertThatSelectOnUpdateIsCorrect(
							sqlStatementInspector.getSqlQueries().get( 1 ),
							found
					);
				}
		);
	}

	private static void assertThatInsertIsCorrect(String insertQuery) {
		assertThat( insertQuery ).contains( NON_GENERATED_PROP_0_COLUMN );
		assertThat( insertQuery ).contains( NON_GENERATED_PROP_1_COLUMN );

		assertThat( insertQuery ).doesNotContain( ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN );
		assertThat( insertQuery ).doesNotContain( INSERT_GENERATED_PROP_WRITABLE_1_COLUMN );
		assertThat( insertQuery ).doesNotContain( ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( insertQuery ).doesNotContain( ALWAYS_GENERATED_PROP_0_COLUMN );
		assertThat( insertQuery ).doesNotContain( ALWAYS_GENERATED_PROP_1_COLUMN );
		assertThat( insertQuery ).doesNotContain( INSERT_GENERATED_PROP_1_COLUMN );
	}

	private static void assertThatSelectOnInsertIsCorrect(String selectQuery, TestEntity testEntity) {
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_0_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_1_COLUMN );

		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN );

		assertThat( selectQuery ).contains( INSERT_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( INSERT_GENERATED_PROP_WRITABLE_1_COLUMN );

		assertThat( selectQuery ).doesNotContain( NON_GENERATED_PROP_0_COLUMN );
//		assertThat( selectQuery ).doesNotContain( NON_GENERATED_PROP_1_COLUMN );

		assertThat( testEntity.getName() ).isEqualTo( NON_GENERATED_PROP_0 );
		assertThat( testEntity.getAlwaysGeneratedProp0() ).isEqualTo( ALWAYS_GENERATED_PROP_0 );
		assertThat( testEntity.getAlwaysGeneratedProp01() ).isEqualTo( ALWAYS_GENERATED_PROP_WRITABLE_0 );

		EmbeddableValue embeddableValue = testEntity.getEmbeddableValue();
		assertThat( embeddableValue.getNonGeneratedProperty1() )
				.isEqualTo( NON_GENERATED_PROP_1 );
		assertThat( embeddableValue.getAlwaysGeneratedProperty1() )
				.isEqualTo( ALWAYS_GENERATED_PROP_1 );
		assertThat( embeddableValue.getAlwaysGeneratedProperty11() )
				.isEqualTo( ALWAYS_GENERATED_PROP_WRITABLE_1 );
		assertThat( embeddableValue.getInsertGeneratedProperty1() )
				.isEqualTo( INSERT_GENERATED_PROP_1 );
		assertThat( embeddableValue.getInsertGeneratedProperty11() )
				.isEqualTo( INSERT_GENERATED_PROP_WRITABLE_1 );
	}

	private static void assertThatSelectStatementIsCorrect(String selectQuery) {
		assertThat( selectQuery ).contains( NON_GENERATED_PROP_0_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_0_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( selectQuery ).contains( NON_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN );
		assertThat( selectQuery ).contains( INSERT_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( INSERT_GENERATED_PROP_WRITABLE_1_COLUMN );
	}

	private static void assertThatUpdateStatementIsCorrect(String updateQuery) {
		assertThat( updateQuery ).contains( NON_GENERATED_PROP_0_COLUMN );

		assertThat( updateQuery ).doesNotContain( ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( updateQuery ).doesNotContain( NON_GENERATED_PROP_1_COLUMN );
		assertThat( updateQuery ).doesNotContain( ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN );
		assertThat( updateQuery ).doesNotContain( INSERT_GENERATED_PROP_WRITABLE_1_COLUMN );

		assertThat( updateQuery ).doesNotContain( ALWAYS_GENERATED_PROP_0_COLUMN );
		assertThat( updateQuery ).doesNotContain( ALWAYS_GENERATED_PROP_1_COLUMN );
		assertThat( updateQuery ).doesNotContain( INSERT_GENERATED_PROP_1_COLUMN );
	}

	private static void assertThatSelectOnUpdateIsCorrect(String selectQuery, TestEntity testEntity) {
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_0_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( selectQuery ).contains( ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN );

		assertThat( selectQuery ).doesNotContain( NON_GENERATED_PROP_0_COLUMN );
//		assertThat( selectQuery ).doesNotContain( NON_GENERATED_PROP_1_COLUMN );
//		assertThat( selectQuery ).contains( INSERT_GENERATED_PROP_WRITABLE_1_COLUMN );
//		assertThat( selectQuery ).doesNotContain( INSERT_GENERATED_PROP_1_COLUMN );

		assertThat( testEntity.getName() ).isEqualTo( UPDATED_NON_GENERATED_PROP_0 );
		assertThat( testEntity.getAlwaysGeneratedProp0() ).isEqualTo( ALWAYS_GENERATED_PROP_0 );
		assertThat( testEntity.getAlwaysGeneratedProp01() ).isEqualTo( ALWAYS_GENERATED_PROP_WRITABLE_0 );

		EmbeddableValue embeddableValue = testEntity.getEmbeddableValue();
		assertThat( embeddableValue.getNonGeneratedProperty1() )
				.isEqualTo( UPDATED_NON_GENERATED_PROP_1 );
		assertThat( embeddableValue.getAlwaysGeneratedProperty1() )
				.isEqualTo( ALWAYS_GENERATED_PROP_1 );
		assertThat( embeddableValue.getAlwaysGeneratedProperty11() )
				.isEqualTo( ALWAYS_GENERATED_PROP_WRITABLE_1 );
		assertThat( embeddableValue.getInsertGeneratedProperty1() )
				.isEqualTo( INSERT_GENERATED_PROP_1 );
		assertThat( embeddableValue.getInsertGeneratedProperty11() )
				.isEqualTo( INSERT_GENERATED_PROP_WRITABLE_1 );
	}

	@Entity(name = "TestEntity")
	@Table(name = "test_entity")
	@DynamicInsert
	@DynamicUpdate
	public static class TestEntity {

		@Id
		private Integer id;
		@Column(name = NON_GENERATED_PROP_0_COLUMN)
		private String name;

		@Column(name = ALWAYS_GENERATED_PROP_0_COLUMN)
		@Generated(GenerationTime.ALWAYS)
		private Integer alwaysGeneratedProp0;

		@Column(name = ALWAYS_GENERATED_PROP_WRITABLE_0_COLUMN)
		@Generated(value = GenerationTime.ALWAYS, writable = true)
		private Integer alwaysGeneratedProp01;

		@Embedded
		private EmbeddableValue embeddableValue;

		public TestEntity() {
		}

		public TestEntity(
				Integer id,
				String name,
				EmbeddableValue embeddableValue) {
			this.id = id;
			this.name = name;
			this.embeddableValue = embeddableValue;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getAlwaysGeneratedProp0() {
			return alwaysGeneratedProp0;
		}

		public void setAlwaysGeneratedProp0(Integer alwaysGeneratedProp0) {
			this.alwaysGeneratedProp0 = alwaysGeneratedProp0;
		}

		public Integer getAlwaysGeneratedProp01() {
			return alwaysGeneratedProp01;
		}

		public void setAlwaysGeneratedProp01(Integer alwaysGeneratedProp01) {
			this.alwaysGeneratedProp01 = alwaysGeneratedProp01;
		}

		public EmbeddableValue getEmbeddableValue() {
			return embeddableValue;
		}

		public void setEmbeddableValue(EmbeddableValue embeddableValue) {
			this.embeddableValue = embeddableValue;
		}
	}

	@Embeddable
	public static class EmbeddableValue {
		@Column(name = NON_GENERATED_PROP_1_COLUMN)
		private Integer nonGeneratedProperty1;

		@Generated(GenerationTime.ALWAYS)
		@Column(name = ALWAYS_GENERATED_PROP_1_COLUMN)
		private Integer alwaysGeneratedProperty1;

		@Generated(value = GenerationTime.ALWAYS, writable = true)
		@Column(name = ALWAYS_GENERATED_PROP_WRITABLE_1_COLUMN)
		private Integer alwaysGeneratedProperty11;

		@Generated(GenerationTime.INSERT)
		@Column(name = INSERT_GENERATED_PROP_1_COLUMN)
		private Integer insertGeneratedProperty1;

		@Generated(value = GenerationTime.INSERT, writable = true)
		@Column(name = INSERT_GENERATED_PROP_WRITABLE_1_COLUMN)
		private Integer insertGeneratedProperty11;

		public Integer getInsertGeneratedProperty1() {
			return insertGeneratedProperty1;
		}

		public void setInsertGeneratedProperty1(Integer insertGeneratedProperty1) {
			this.insertGeneratedProperty1 = insertGeneratedProperty1;
		}

		public Integer getInsertGeneratedProperty11() {
			return insertGeneratedProperty11;
		}

		public void setInsertGeneratedProperty11(Integer insertGeneratedProperty11) {
			this.insertGeneratedProperty11 = insertGeneratedProperty11;
		}

		public EmbeddableValue() {
		}

		public EmbeddableValue(Integer nonGeneratedProperty1) {
			this.nonGeneratedProperty1 = nonGeneratedProperty1;
		}


		public Integer getNonGeneratedProperty1() {
			return nonGeneratedProperty1;
		}

		public void setNonGeneratedProperty1(Integer nonGeneratedProperty1) {
			this.nonGeneratedProperty1 = nonGeneratedProperty1;
		}

		public Integer getAlwaysGeneratedProperty1() {
			return alwaysGeneratedProperty1;
		}

		public void setAlwaysGeneratedProperty1(Integer alwaysGeneratedProperty1) {
			this.alwaysGeneratedProperty1 = alwaysGeneratedProperty1;
		}

		public Integer getAlwaysGeneratedProperty11() {
			return alwaysGeneratedProperty11;
		}

		public void setAlwaysGeneratedProperty11(Integer alwaysGeneratedProperty11) {
			this.alwaysGeneratedProperty11 = alwaysGeneratedProperty11;
		}
	}
}
