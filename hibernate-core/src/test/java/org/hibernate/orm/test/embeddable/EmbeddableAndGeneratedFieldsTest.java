package org.hibernate.orm.test.embeddable;

import java.sql.PreparedStatement;

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
				EmbeddableAndGeneratedFieldsTest.TestEntity.class
		}
)
@SessionFactory(
		exportSchema = false,
		useCollectingStatementInspector = true
)
@Jira("HHH-16957")
public class EmbeddableAndGeneratedFieldsTest {
	private final static String NAME = "a";
	private final static String UPDATED_NAME = "b";

	private final static int NON_GENERATED_PROP_0 = 1;
	private final static int NON_GENERATED_PROP_1 = 20;
	private final static int NON_GENERATED_PROP_2 = 21;
	private final static int NON_GENERATED_PROP_3 = 22;

	private final static int NEVER_GENERATED_PROP_0 = 30;
	private final static int UPDATED_NEVER_GENERATED_PROP_0 = 40;
	private final static int NEVER_GENERATED_PROP_1 = 31;
	private final static int UPDATED_NEVER_GENERATED_PROP_1 = 41;

	private final static int ALWAYS_GENERATED_PROP_1 = NON_GENERATED_PROP_1 + 1;
	private final static int ALWAYS_GENERATED_PROP_2 = NON_GENERATED_PROP_1 + 2;
	private final static int ALWAYS_GENERATED_PROP_21 = NON_GENERATED_PROP_1 + 3;

	private final static int INSERT_GENERATED_PROP_0 = NON_GENERATED_PROP_1 + 4;
	private final static int INSERT_GENERATED_PROP_1 = NON_GENERATED_PROP_1 + 5;

	private final static int UPDATE_GENERATED_PROP_0 = NON_GENERATED_PROP_1 + 6;
	private final static int UPDATE_GENERATED_PROP_1 = NON_GENERATED_PROP_1 + 7;
	private final static int UPDATE_GENERATED_PROP_3 = NON_GENERATED_PROP_1 + 8;
	private final static int UPDATE_GENERATED_PROP_31 = NON_GENERATED_PROP_1 + 9;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork( connection -> {
							PreparedStatement createTablePreparedStatement = connection.prepareStatement(
									"create table IF NOT EXISTS  test_entity (" +
											" id integer not null," +
											" name varchar(255)," +
											" insert_generated_prop_0 integer," +
											" update_generated_prop_0 integer, " +
											" never_generated_prop_0 integer, " +

											" non_generated_prop_1 integer, " +
											" always_generated_prop_1 integer generated always as ( " + ALWAYS_GENERATED_PROP_1 + " ) stored," +
											" update_generated_prop_1 integer, " +
											" insert_generated_prop_1 integer," +
											" never_generated_prop_1 integer, " +

											" non_generated_prop_2 integer, " +
											" always_generated_prop_2 integer generated always as ( " + ALWAYS_GENERATED_PROP_2 + " ) stored, " +
											" always_generated_prop_21 integer generated always as ( " + ALWAYS_GENERATED_PROP_21 + " ) stored, " +

											" non_generated_prop_3 integer, " +
											" update_generated_prop_3 integer, " +
											" update_generated_prop_31 integer, " +

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
											"insert_generated_prop_0 = " + INSERT_GENERATED_PROP_0 + " , " +
											"insert_generated_prop_1 = " + INSERT_GENERATED_PROP_1 +
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

							PreparedStatement afterUpdateFunctionPreparedStatement = connection.prepareStatement(
									"create or replace function afterNameUpdate() returns trigger as  $afterNameUpdate$ begin " +
											"update test_entity set " +
											"update_generated_prop_0 = " + UPDATE_GENERATED_PROP_0 + " , " +
											"update_generated_prop_1 =  " + UPDATE_GENERATED_PROP_1 + " , " +
											"update_generated_prop_3 = " + UPDATE_GENERATED_PROP_3 + " , " +
											"update_generated_prop_31 = " + UPDATE_GENERATED_PROP_31 +
											" where id = NEW.id and NEW.update_generated_prop_0 is null ; return NEW; END; $afterNameUpdate$ LANGUAGE plpgsql" );
							try {
								afterUpdateFunctionPreparedStatement.execute();
							}
							finally {
								afterUpdateFunctionPreparedStatement.close();
							}

							PreparedStatement afterUpdateTriggerPreparedStatement = connection.prepareStatement(
									"create or replace trigger after_name_update_t after update on test_entity for each row execute function afterNameUpdate();" );
							try {
								afterUpdateTriggerPreparedStatement.execute();
							}
							finally {
								afterUpdateTriggerPreparedStatement.close();
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

							PreparedStatement dropAfterUpdateTrigger = connection
									.prepareStatement( "drop trigger IF EXISTS after_name_update_t on test_entity" );
							try {
								dropAfterUpdateTrigger.execute();
							}
							finally {
								dropAfterUpdateTrigger.close();
							}

							PreparedStatement dropAfterInsertFunction = connection
									.prepareStatement( "drop function IF EXISTS afterInsert" );
							try {
								dropAfterInsertFunction.execute();
							}
							finally {
								dropAfterInsertFunction.close();
							}

							PreparedStatement dropAfterUpdateFunction = connection
									.prepareStatement( "drop function IF EXISTS afterNameUpdate" );
							try {
								dropAfterUpdateFunction.execute();
							}
							finally {
								dropAfterUpdateFunction.close();
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
				NAME,
				NEVER_GENERATED_PROP_0,
				new EmbeddableValue( NON_GENERATED_PROP_1, NEVER_GENERATED_PROP_1 ),
				new EmbeddableValue2( NON_GENERATED_PROP_2 ),
				new EmbeddableValue3( NON_GENERATED_PROP_3 )
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
					assertThatFindIsCorrect( sqlStatementInspector.getSqlQueries().get( 0 ) );

					sqlStatementInspector.clear();

					found.setName( UPDATED_NAME );
					found.setNeverGeneratedProp0( UPDATED_NEVER_GENERATED_PROP_0 );
					found.getEmbeddableValue().setNeverGeneratedProp1( UPDATED_NEVER_GENERATED_PROP_1 );
					session.flush();

					sqlStatementInspector.assertExecutedCount( 2 );
					sqlStatementInspector.assertIsUpdate( 0 );
					assertThatUpdateIsCorrect( sqlStatementInspector.getSqlQueries().get( 0 ) );
					assertThatSelectOnUpdateIsCorrect(
							sqlStatementInspector.getSqlQueries().get( 1 ),
							found
					);
				}
		);
	}

	private static void assertThatInsertIsCorrect(String insertQuery) {
		assertThat( insertQuery ).contains( "name" );

		assertThat( insertQuery ).contains( "non_generated_prop_1" );
		assertThat( insertQuery ).contains( "non_generated_prop_2" );
		assertThat( insertQuery ).contains( "non_generated_prop_3" );

		assertThat( insertQuery ).contains( "never_generated_prop_0" );
		assertThat( insertQuery ).contains( "never_generated_prop_1" );

		assertThat( insertQuery ).doesNotContain( "update_generated_prop_0" );
		assertThat( insertQuery ).doesNotContain( "update_generated_prop_1" );
		assertThat( insertQuery ).doesNotContain( "update_generated_prop_3" );
		assertThat( insertQuery ).doesNotContain( "update_generated_prop_31" );

		assertThat( insertQuery ).doesNotContain( "insert_generated_prop_0" );
		assertThat( insertQuery ).doesNotContain( "insert_generated_prop_1" );

		assertThat( insertQuery ).doesNotContain( "always_generated_prop_1" );
		assertThat( insertQuery ).doesNotContain( "always_generated_prop_2" );
		assertThat( insertQuery ).doesNotContain( "always_generated_prop_21" );
	}

	private static void assertThatUpdateIsCorrect(String updateQuery) {
		assertThat( updateQuery ).contains( "name" );

		assertThat( updateQuery ).contains( "non_generated_prop_1" );
		assertThat( updateQuery ).contains( "non_generated_prop_2" );
		assertThat( updateQuery ).contains( "non_generated_prop_3" );

		assertThat( updateQuery ).contains( "never_generated_prop_0" );
		assertThat( updateQuery ).contains( "never_generated_prop_1" );

		assertThat( updateQuery ).contains( "insert_generated_prop_0" );
		assertThat( updateQuery ).contains( "insert_generated_prop_1" );

		assertThat( updateQuery ).doesNotContain( "update_generated_prop_0" );
		assertThat( updateQuery ).doesNotContain( "update_generated_prop_1" );
		assertThat( updateQuery ).doesNotContain( "update_generated_prop_3" );
		assertThat( updateQuery ).doesNotContain( "update_generated_prop_31" );

		assertThat( updateQuery ).doesNotContain( "always_generated_prop_1" );
		assertThat( updateQuery ).doesNotContain( "always_generated_prop_2" );
		assertThat( updateQuery ).doesNotContain( "always_generated_prop_21" );
	}

	private static void assertThatSelectOnUpdateIsCorrect(String selectQuery, TestEntity testEntity) {
		assertThat( selectQuery ).contains( "always_generated_prop_1" );
		assertThat( selectQuery ).contains( "always_generated_prop_2" );
		assertThat( selectQuery ).contains( "always_generated_prop_21" );
		assertThat( selectQuery ).contains( "update_generated_prop_0" );
		assertThat( selectQuery ).contains( "update_generated_prop_1" );
		assertThat( selectQuery ).contains( "update_generated_prop_3" );

		assertThat( selectQuery ).doesNotContain( "insert_generated_prop_0" );
		assertThat( selectQuery ).doesNotContain( "insert_generated_prop_0" );

		assertThat( selectQuery ).doesNotContain( "non_generated_prop" );
		assertThat( selectQuery ).doesNotContain( "never_generated_prop_0" );
		assertThat( selectQuery ).doesNotContain( "never_generated_prop_1" );

		assertThat( testEntity.getName() ).isEqualTo( UPDATED_NAME );
		assertThat( testEntity.getInsertGeneratedProp0() ).isEqualTo( INSERT_GENERATED_PROP_0 );
		assertThat( testEntity.getUpdateGenerateProp0() ).isEqualTo( UPDATE_GENERATED_PROP_0 );
		assertThat( testEntity.getNeverGeneratedProp0() ).isEqualTo( UPDATED_NEVER_GENERATED_PROP_0 );

		EmbeddableValue embeddableValue = testEntity.getEmbeddableValue();
		assertThat( embeddableValue.getNonGeneratedProperty1() )
				.isEqualTo( NON_GENERATED_PROP_1 );
		assertThat( embeddableValue.getInsertGeneratedProperty1() )
				.isEqualTo( INSERT_GENERATED_PROP_1 );
		assertThat( embeddableValue.getAlwaysGeneratedProperty1() )
				.isEqualTo( ALWAYS_GENERATED_PROP_1 );
		assertThat( embeddableValue.getUpdateGeneratedProperty1() )
				.isEqualTo( UPDATE_GENERATED_PROP_1 );
		assertThat( embeddableValue.getNeverGeneratedProp1() )
				.isEqualTo( UPDATED_NEVER_GENERATED_PROP_1 );

		EmbeddableValue2 embeddableValue2 = testEntity.getEmbeddableValue2();
		assertThat( embeddableValue2 ).isNotNull();
		assertThat( embeddableValue2.getNonGeneratedProperty2() )
				.isEqualTo( NON_GENERATED_PROP_2 );
		assertThat( embeddableValue2.getAlwaysGeneratedProperty2() )
				.isEqualTo( ALWAYS_GENERATED_PROP_2 );
		assertThat( embeddableValue2.getAlwaysGeneratedProperty21() )
				.isEqualTo( ALWAYS_GENERATED_PROP_21 );

		EmbeddableValue3 embeddableValue3 = testEntity.getEmbeddableValue3();
		assertThat( embeddableValue3 ).isNotNull();
		assertThat( embeddableValue3.getNonGeneratedProperty3() )
				.isEqualTo( NON_GENERATED_PROP_3 );
		assertThat( embeddableValue3.getUpdateGeneratedProperty3() )
				.isEqualTo( UPDATE_GENERATED_PROP_3 );
		assertThat( embeddableValue3.getUpdateGeneratedProperty31() )
				.isEqualTo( UPDATE_GENERATED_PROP_31 );
	}

	private static void assertThatSelectOnInsertIsCorrect(String selectQuery, TestEntity testEntity) {

		assertThat( selectQuery ).contains( "insert_generated_prop_0" );
		assertThat( selectQuery ).contains( "insert_generated_prop_0" );
		assertThat( selectQuery ).contains( "always_generated_prop_1" );
		assertThat( selectQuery ).contains( "always_generated_prop_2" );
		assertThat( selectQuery ).contains( "always_generated_prop_21" );

		assertThat( selectQuery ).doesNotContain( "update_generated_prop_0" );
		assertThat( selectQuery ).doesNotContain( "update_generated_prop_1" );
		assertThat( selectQuery ).doesNotContain( "update_generated_prop_3" );

		assertThat( selectQuery ).doesNotContain( "never_generated_prop_0" );
		assertThat( selectQuery ).doesNotContain( "never_generated_prop_1" );
		assertThat( selectQuery ).doesNotContain( "non_generated_prop" );

		assertThat( testEntity.getName() ).isEqualTo( NAME );
		assertThat( testEntity.getInsertGeneratedProp0() ).isEqualTo( INSERT_GENERATED_PROP_0 );
		assertThat( testEntity.getUpdateGenerateProp0() ).isNull();
		assertThat( testEntity.getNeverGeneratedProp0() ).isEqualTo( NEVER_GENERATED_PROP_0 );

		EmbeddableValue embeddableValue = testEntity.getEmbeddableValue();
		assertThat( embeddableValue.getNonGeneratedProperty1() )
				.isEqualTo( NON_GENERATED_PROP_1 );
		assertThat( embeddableValue.getInsertGeneratedProperty1() )
				.isEqualTo( INSERT_GENERATED_PROP_1 );
		assertThat( embeddableValue.getAlwaysGeneratedProperty1() )
				.isEqualTo( ALWAYS_GENERATED_PROP_1 );
		assertThat( embeddableValue.getUpdateGeneratedProperty1() )
				.isNull();
		assertThat( embeddableValue.getNeverGeneratedProp1() )
				.isEqualTo( NEVER_GENERATED_PROP_1 );

		EmbeddableValue2 embeddableValue2 = testEntity.getEmbeddableValue2();
		assertThat( embeddableValue2 ).isNotNull();
		assertThat( embeddableValue2.getNonGeneratedProperty2() )
				.isEqualTo( NON_GENERATED_PROP_2 );
		assertThat( embeddableValue2.getAlwaysGeneratedProperty2() )
				.isEqualTo( ALWAYS_GENERATED_PROP_2 );
		assertThat( embeddableValue2.getAlwaysGeneratedProperty21() )
				.isEqualTo( ALWAYS_GENERATED_PROP_21 );

		EmbeddableValue3 embeddableValue3 = testEntity.getEmbeddableValue3();
		assertThat( embeddableValue3 ).isNotNull();
		assertThat( embeddableValue3.getNonGeneratedProperty3() )
				.isEqualTo( NON_GENERATED_PROP_3 );
		assertThat( embeddableValue3.getUpdateGeneratedProperty3() )
				.isNull();
		assertThat( embeddableValue3.getUpdateGeneratedProperty31() )
				.isNull();
	}

	private static void assertThatFindIsCorrect(String selectQuery) {
		assertThat( selectQuery ).contains( "name" );
		assertThat( selectQuery ).contains( "insert_generated_prop_0" );
		assertThat( selectQuery ).contains( "update_generated_prop_0" );
		assertThat( selectQuery ).contains( "never_generated_prop_0" );

		assertThat( selectQuery ).contains( "non_generated_prop_1" );
		assertThat( selectQuery ).contains( "always_generated_prop_1" );
		assertThat( selectQuery ).contains( "update_generated_prop_1" );
		assertThat( selectQuery ).contains( "insert_generated_prop_1" );
		assertThat( selectQuery ).contains( "never_generated_prop_1" );

		assertThat( selectQuery ).contains( "non_generated_prop_2" );
		assertThat( selectQuery ).contains( "always_generated_prop_2" );
		assertThat( selectQuery ).contains( "always_generated_prop_21" );

		assertThat( selectQuery ).contains( "non_generated_prop_3" );
		assertThat( selectQuery ).contains( "update_generated_prop_3" );
		assertThat( selectQuery ).contains( "update_generated_prop_31" );
	}

	@Entity(name = "TestEntity")
	@Table(name = "test_entity")
	public static class TestEntity {

		private Integer id;

		private String name;

		private Integer insertGeneratedProp0;

		private Integer updateGenerateProp0;

		private Integer neverGeneratedProp0;

		private EmbeddableValue embeddableValue;

		private EmbeddableValue2 embeddableValue2;

		private EmbeddableValue3 embeddableValue3;

		public TestEntity() {
		}

		public TestEntity(
				Integer id,
				String name,
				Integer neverGeneratedProp,
				EmbeddableValue embeddableValue,
				EmbeddableValue2 anotherEmbeddableValue,
				EmbeddableValue3 embeddableValue3) {
			this.id = id;
			this.name = name;
			this.neverGeneratedProp0 = neverGeneratedProp;
			this.embeddableValue = embeddableValue;
			this.embeddableValue2 = anotherEmbeddableValue;
			this.embeddableValue3 = embeddableValue3;
		}

		@Id
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

		@Column(name = "insert_generated_prop_0")
		@Generated(GenerationTime.INSERT)
		public Integer getInsertGeneratedProp0() {
			return insertGeneratedProp0;
		}

		public void setInsertGeneratedProp0(Integer insertGeneratedProp0) {
			this.insertGeneratedProp0 = insertGeneratedProp0;
		}

		@Column(name = "update_generated_prop_0")
		@Generated(GenerationTime.UPDATE)
		public Integer getUpdateGenerateProp0() {
			return updateGenerateProp0;
		}

		public void setUpdateGenerateProp0(Integer updateGenerateProp0) {
			this.updateGenerateProp0 = updateGenerateProp0;
		}

		@Column(name = "never_generated_prop_0")
		@Generated(GenerationTime.NEVER)
		public Integer getNeverGeneratedProp0() {
			return neverGeneratedProp0;
		}

		public void setNeverGeneratedProp0(Integer neverGeneratedProp0) {
			this.neverGeneratedProp0 = neverGeneratedProp0;
		}

		@Embedded
		public EmbeddableValue getEmbeddableValue() {
			return embeddableValue;
		}

		public void setEmbeddableValue(EmbeddableValue embeddableValue) {
			this.embeddableValue = embeddableValue;
		}

		@Embedded
		public EmbeddableValue2 getEmbeddableValue2() {
			return embeddableValue2;
		}

		public void setEmbeddableValue2(EmbeddableValue2 embeddableValue2) {
			this.embeddableValue2 = embeddableValue2;
		}

		@Embedded
		public EmbeddableValue3 getEmbeddableValue3() {
			return embeddableValue3;
		}

		public void setEmbeddableValue3(EmbeddableValue3 embeddableValue3) {
			this.embeddableValue3 = embeddableValue3;
		}
	}

	@Embeddable
	public static class EmbeddableValue {
		private Integer nonGeneratedProperty1;

		private Integer alwaysGeneratedProperty1;

		private Integer updateGeneratedProperty1;

		private Integer insertGeneratedProperty1;

		private Integer neverGeneratedProp1;

		public EmbeddableValue() {
		}

		public EmbeddableValue(Integer nonGeneratedProperty, Integer neverGeneratedProp) {
			this.nonGeneratedProperty1 = nonGeneratedProperty;
			this.neverGeneratedProp1 = neverGeneratedProp;
		}

		@Column(name = "non_generated_prop_1")
		public Integer getNonGeneratedProperty1() {
			return nonGeneratedProperty1;
		}

		public void setNonGeneratedProperty1(Integer nonGeneratedProperty1) {
			this.nonGeneratedProperty1 = nonGeneratedProperty1;
		}

		@Generated(GenerationTime.ALWAYS)
		@Column(name = "always_generated_prop_1")
		public Integer getAlwaysGeneratedProperty1() {
			return alwaysGeneratedProperty1;
		}

		public void setAlwaysGeneratedProperty1(Integer alwaysGeneratedProperty1) {
			this.alwaysGeneratedProperty1 = alwaysGeneratedProperty1;
		}

		@Generated(GenerationTime.UPDATE)
		@Column(name = "update_generated_prop_1")
		public Integer getUpdateGeneratedProperty1() {
			return updateGeneratedProperty1;
		}

		public void setUpdateGeneratedProperty1(Integer updateGeneratedProperty1) {
			this.updateGeneratedProperty1 = updateGeneratedProperty1;
		}

		@Generated(GenerationTime.INSERT)
		@Column(name = "insert_generated_prop_1")
		public Integer getInsertGeneratedProperty1() {
			return insertGeneratedProperty1;
		}

		public void setInsertGeneratedProperty1(Integer insertGeneratedProperty1) {
			this.insertGeneratedProperty1 = insertGeneratedProperty1;
		}

		@Column(name = "never_generated_prop_1")
		@Generated(GenerationTime.NEVER)
		public Integer getNeverGeneratedProp1() {
			return neverGeneratedProp1;
		}

		public void setNeverGeneratedProp1(Integer neverGeneratedProp1) {
			this.neverGeneratedProp1 = neverGeneratedProp1;
		}
	}

	@Embeddable
	public static class EmbeddableValue2 {
		private Integer nonGeneratedProperty2;

		private Integer alwaysGeneratedProperty2;

		private Integer alwaysGeneratedProperty21;

		public EmbeddableValue2() {
		}

		public EmbeddableValue2(Integer nonGeneratedProperty2) {
			this.nonGeneratedProperty2 = nonGeneratedProperty2;
		}

		@Column(name = "non_generated_prop_2")
		public Integer getNonGeneratedProperty2() {
			return nonGeneratedProperty2;
		}

		public void setNonGeneratedProperty2(Integer nonGeneratedProperty2) {
			this.nonGeneratedProperty2 = nonGeneratedProperty2;
		}

		@Generated(GenerationTime.ALWAYS)
		@Column(name = "always_generated_prop_2")
		public Integer getAlwaysGeneratedProperty2() {
			return alwaysGeneratedProperty2;
		}

		public void setAlwaysGeneratedProperty2(Integer alwaysGeneratedProperty2) {
			this.alwaysGeneratedProperty2 = alwaysGeneratedProperty2;
		}

		@Generated(GenerationTime.ALWAYS)
		@Column(name = "always_generated_prop_21")
		public Integer getAlwaysGeneratedProperty21() {
			return alwaysGeneratedProperty21;
		}

		public void setAlwaysGeneratedProperty21(Integer alwaysGeneratedProperty21) {
			this.alwaysGeneratedProperty21 = alwaysGeneratedProperty21;
		}
	}

	@Embeddable
	public static class EmbeddableValue3 {
		private Integer nonGeneratedProperty3;

		private Integer updateGeneratedProperty3;

		private Integer updateGeneratedProperty31;

		public EmbeddableValue3() {
		}

		public EmbeddableValue3(Integer nonGeneratedProperty3) {
			this.nonGeneratedProperty3 = nonGeneratedProperty3;
		}

		@Column(name = "non_generated_prop_3")
		public Integer getNonGeneratedProperty3() {
			return nonGeneratedProperty3;
		}

		public void setNonGeneratedProperty3(Integer nonGeneratedProperty3) {
			this.nonGeneratedProperty3 = nonGeneratedProperty3;
		}

		@Generated(GenerationTime.UPDATE)
		@Column(name = "update_generated_prop_3")
		public Integer getUpdateGeneratedProperty3() {
			return updateGeneratedProperty3;
		}

		public void setUpdateGeneratedProperty3(Integer updateGeneratedProperty3) {
			this.updateGeneratedProperty3 = updateGeneratedProperty3;
		}

		@Generated(GenerationTime.UPDATE)
		@Column(name = "update_generated_prop_31")
		public Integer getUpdateGeneratedProperty31() {
			return updateGeneratedProperty31;
		}

		public void setUpdateGeneratedProperty31(Integer updateGeneratedProperty31) {
			this.updateGeneratedProperty31 = updateGeneratedProperty31;
		}
	}

}
