package org.hibernate.orm.test.embeddable.generated;

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
				UpdateGeneratedTest.TestEntity.class
		}
)
@SessionFactory(
		exportSchema = false,
		useCollectingStatementInspector = true
)
@Jira("HHH-16957")
public class UpdateGeneratedTest {
	private final static String NON_GENERATED_PROP_0 = "a";
	private final static String UPDATED_NON_GENERATED_PROP_0 = "b";
	private final static int NON_GENERATED_PROP_1 = 20;
	private final static int UPDATED_NON_GENERATED_PROP_1 = 20;

	private final static int UPDATE_GENERATED_PROP_0 = 1;
	private final static int UPDATE_GENERATED_PROP_WRITABLE_0 = 2;
	private final static int UPDATE_GENERATED_PROP_1 = 3;
	private final static int UPDATE_GENERATED_PROP_WRITABLE_1 = 4;

	private final static String NON_GENERATED_PROP_0_COLUMN = "non_generate_prop_0";
	private final static String UPDATE_GENERATED_PROP_0_COLUMN = "update_generated_prop_0";
	private final static String UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN = "update_generated_prop_writable_0";

	private final static String NON_GENERATED_PROP_1_COLUMN = "non_generate_prop_1";
	private final static String UPDATE_GENERATED_PROP_1_COLUMN = "update_generated_prop_1";
	private final static String UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN = "update_generated_prop_writable_1";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork( connection -> {
							PreparedStatement createTablePreparedStatement = connection.prepareStatement(
									"create table IF NOT EXISTS test_entity (" +
											" id integer not null, " +
											NON_GENERATED_PROP_0_COLUMN + " varchar(255), " +
											UPDATE_GENERATED_PROP_0_COLUMN + " integer , " +
											UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN + " integer, " +
											NON_GENERATED_PROP_1_COLUMN + " integer, " +
											UPDATE_GENERATED_PROP_1_COLUMN + " integer , " +
											UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN + " integer, " +
											" primary key (id)" +
											" )" );
							try {
								createTablePreparedStatement.execute();
							}
							finally {
								createTablePreparedStatement.close();
							}

							PreparedStatement afterUpdateFunctionPreparedStatement = connection.prepareStatement(
									"create or replace function afterUpdate() returns trigger as  $afterUpdate$ begin " +
											"update test_entity set " +
											UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN + " = " + UPDATE_GENERATED_PROP_WRITABLE_0 + " , " +
											UPDATE_GENERATED_PROP_0_COLUMN + " = " + UPDATE_GENERATED_PROP_0 + " , " +
											UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN + " =  " + UPDATE_GENERATED_PROP_WRITABLE_1 + " , " +
											UPDATE_GENERATED_PROP_1_COLUMN + " = " + UPDATE_GENERATED_PROP_1 +
											" where id = NEW.id and NEW." + UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN + " is null ; " +
											"return NEW; END; $afterUpdate$ LANGUAGE plpgsql" );
							try {
								afterUpdateFunctionPreparedStatement.execute();
							}
							finally {
								afterUpdateFunctionPreparedStatement.close();
							}

							PreparedStatement afterUpdateTriggerPreparedStatement = connection.prepareStatement(
									"create or replace trigger after_update_t after update on test_entity for each row execute function afterUpdate();" );
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
									.prepareStatement( "drop trigger IF EXISTS after_update_t on test_entity" );
							try {
								dropAfterInsertTrigger.execute();
							}
							finally {
								dropAfterInsertTrigger.close();
							}

							PreparedStatement dropAfterInsertFunction = connection
									.prepareStatement( "drop function IF EXISTS afterUpdate" );
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

		sqlStatementInspector.assertExecutedCount( 1 );
		sqlStatementInspector.assertIsInsert( 0 );

		assertThatInsertIsCorrect( sqlStatementInspector.getSqlQueries().get( 0 ) );

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
		assertThat( insertQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( insertQuery ).contains( NON_GENERATED_PROP_1_COLUMN );
		assertThat( insertQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN );

		assertThat( insertQuery ).doesNotContain( UPDATE_GENERATED_PROP_0_COLUMN );
		assertThat( insertQuery ).doesNotContain( UPDATE_GENERATED_PROP_1_COLUMN );
	}

	private static void assertThatSelectStatementIsCorrect(String selectQuery) {
		assertThat( selectQuery ).contains( NON_GENERATED_PROP_0_COLUMN );
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_0_COLUMN );
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( selectQuery ).contains( NON_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN );
	}

	private static void assertThatUpdateStatementIsCorrect(String updateQuery) {
		assertThat( updateQuery ).contains( NON_GENERATED_PROP_0_COLUMN );

		assertThat( updateQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( updateQuery ).contains( NON_GENERATED_PROP_1_COLUMN );
		assertThat( updateQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN );

		assertThat( updateQuery ).doesNotContain( UPDATE_GENERATED_PROP_0_COLUMN );
		assertThat( updateQuery ).doesNotContain( UPDATE_GENERATED_PROP_1_COLUMN );
	}

	private static void assertThatSelectOnUpdateIsCorrect(String selectQuery, TestEntity testEntity) {
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_0_COLUMN );
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_1_COLUMN );
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN );
		assertThat( selectQuery ).contains( UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN );

		assertThat( selectQuery ).doesNotContain( NON_GENERATED_PROP_0_COLUMN );
//		assertThat( selectQuery ).doesNotContain( NON_GENERATED_PROP_1_COLUMN );

		assertThat( testEntity.getName() ).isEqualTo( UPDATED_NON_GENERATED_PROP_0 );
		assertThat( testEntity.getUpdateGeneratedProp0() ).isEqualTo( UPDATE_GENERATED_PROP_0 );
		assertThat( testEntity.getUpdateGeneratedProp01() ).isEqualTo( UPDATE_GENERATED_PROP_WRITABLE_0 );

		EmbeddableValue embeddableValue = testEntity.getEmbeddableValue();
		assertThat( embeddableValue.getNonGeneratedProperty1() )
				.isEqualTo( UPDATED_NON_GENERATED_PROP_1 );
		assertThat( embeddableValue.getUpdateGeneratedProperty1() )
				.isEqualTo( UPDATE_GENERATED_PROP_1 );
		assertThat( embeddableValue.getUpdateGeneratedProperty11() )
				.isEqualTo( UPDATE_GENERATED_PROP_WRITABLE_1 );
	}

	@Entity(name = "TestEntity")
	@Table(name = "test_entity")
	public static class TestEntity {

		@Id
		private Integer id;
		@Column(name = NON_GENERATED_PROP_0_COLUMN)
		private String name;

		@Column(name = UPDATE_GENERATED_PROP_0_COLUMN)
		@Generated(GenerationTime.UPDATE)
		private Integer updateGeneratedProp0;

		@Column(name = UPDATE_GENERATED_PROP_WRITABLE_0_COLUMN)
		@Generated(value = GenerationTime.UPDATE, writable = true)
		private Integer updateGeneratedProp01;

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

		public Integer getUpdateGeneratedProp0() {
			return updateGeneratedProp0;
		}

		public void setUpdateGeneratedProp0(Integer updateGeneratedProp0) {
			this.updateGeneratedProp0 = updateGeneratedProp0;
		}

		public Integer getUpdateGeneratedProp01() {
			return updateGeneratedProp01;
		}

		public void setUpdateGeneratedProp01(Integer updateGeneratedProp01) {
			this.updateGeneratedProp01 = updateGeneratedProp01;
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

		@Generated(GenerationTime.UPDATE)
		@Column(name = UPDATE_GENERATED_PROP_1_COLUMN)
		private Integer updateGeneratedProperty1;

		@Generated(value = GenerationTime.UPDATE, writable = true)
		@Column(name = UPDATE_GENERATED_PROP_WRITABLE_1_COLUMN)
		private Integer updateGeneratedProperty11;

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

		public Integer getUpdateGeneratedProperty1() {
			return updateGeneratedProperty1;
		}

		public void setUpdateGeneratedProperty1(Integer updateGeneratedProperty1) {
			this.updateGeneratedProperty1 = updateGeneratedProperty1;
		}

		public Integer getUpdateGeneratedProperty11() {
			return updateGeneratedProperty11;
		}

		public void setUpdateGeneratedProperty11(Integer updateGeneratedProperty11) {
			this.updateGeneratedProperty11 = updateGeneratedProperty11;
		}
	}
}
