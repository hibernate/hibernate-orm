package org.hibernate.orm.test.bytecode.enhancement.secondarytables;

import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey("HHH-17587")
@DomainModel(
		annotatedClasses = {
				SecondaryTableDynamicUpateTest.TestEntity.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class SecondaryTableDynamicUpateTest {

	private static final Long ENTITY_ID = 123l;
	private static final String COL_VALUE = "col";
	private static final String COL1_VALUE = "col1";
	private static final String COL2_VALUE = "col2";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = new TestEntity( ENTITY_ID, COL_VALUE, COL1_VALUE, COL2_VALUE );
					entityManager.persist( testEntity );
				}
		);
	}

	@Test
	public void testSetSecondaryTableColumnToNull(SessionFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = entityManager.find( TestEntity.class, ENTITY_ID );
					assertThat( testEntity.getTestCol() ).isEqualTo( COL_VALUE );
					assertThat( testEntity.getTestCol1() ).isEqualTo( COL1_VALUE );
					assertThat( testEntity.getTestCol2() ).isEqualTo( COL2_VALUE );
					testEntity.setTestCol1( null );
				}
		);

		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = entityManager.find( TestEntity.class, ENTITY_ID );
					assertThat( testEntity ).isNotNull();
					assertThat( testEntity.getTestCol() ).isEqualTo( COL_VALUE );
					assertThat( testEntity.getTestCol1() ).isNull();
					assertThat( testEntity.getTestCol2() ).isEqualTo( COL2_VALUE );
					testEntity.setTestCol2( null );
				}
		);

		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = entityManager.find( TestEntity.class, ENTITY_ID );
					assertThat( testEntity ).isNotNull();
					assertThat( testEntity.getTestCol() ).isEqualTo( COL_VALUE );
					assertThat( testEntity.getTestCol1() ).isNull();
					assertThat( testEntity.getTestCol2() ).isNull();
					testEntity.setTestCol1( COL1_VALUE );
					testEntity.setTestCol( null );
				}
		);

		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = entityManager.find( TestEntity.class, ENTITY_ID );
					assertThat( testEntity ).isNotNull();
					assertThat( testEntity.getTestCol() ).isNull();
					assertThat( testEntity.getTestCol1() ).isEqualTo( COL1_VALUE );
					assertThat( testEntity.getTestCol2() ).isNull();
					testEntity.setTestCol2( null );
				}
		);
	}

	@Entity(name = "TestEntity")
	@SecondaryTable(name = "SECOND_TABLE_TEST", pkJoinColumns = @PrimaryKeyJoinColumn(name = "ID"))
	@DynamicUpdate
	public static class TestEntity {

		@Id
		private Long id;

		@Column(name = "TEST_COL")
		private String testCol;

		@Column(name = "TESTCOL1", table = "SECOND_TABLE_TEST")
		private String testCol1;

		@Column(name = "TESTCOL2", table = "SECOND_TABLE_TEST")
		private String testCol2;

		public TestEntity() {
		}

		public TestEntity(Long id, String testCol, String testCol1, String testCol2) {
			this.id = id;
			this.testCol = testCol;
			this.testCol1 = testCol1;
			this.testCol2 = testCol2;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTestCol() {
			return testCol;
		}

		public void setTestCol(String testCol) {
			this.testCol = testCol;
		}

		public String getTestCol1() {
			return testCol1;
		}

		public void setTestCol1(String testCol1) {
			this.testCol1 = testCol1;
		}

		public String getTestCol2() {
			return testCol2;
		}

		public void setTestCol2(String testCol2) {
			this.testCol2 = testCol2;
		}

	}
}
