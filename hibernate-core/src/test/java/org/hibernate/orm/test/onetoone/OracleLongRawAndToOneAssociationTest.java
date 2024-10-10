package org.hibernate.orm.test.onetoone;


import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				OracleLongRawAndToOneAssociationTest.Detail.class,
				OracleLongRawAndToOneAssociationTest.Master.class
		}
)
@SessionFactory(exportSchema = false)
@RequiresDialect(OracleDialect.class)
@JiraKey("HHH-17733")
public class OracleLongRawAndToOneAssociationTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeQuery(
									"CREATE TABLE HIB_TEST_MASTER(ID NUMBER(10,0), ID_DETAIL NUMBER(10,0) not null unique, BINARY_DATA LONG RAW, NUMBER_COLUMN NUMBER(10,0))" )
							.executeUpdate();
					session.createNativeQuery(
									"INSERT INTO HIB_TEST_MASTER VALUES (1, 1, UTL_RAW.CAST_TO_RAW('binary data...'), 2)" )
							.executeUpdate();

					session.createNativeQuery( "CREATE  TABLE HIB_TEST_DETAIL(ID NUMBER(10,0), NAME VARCHAR2(20))" )
							.executeUpdate();
					session.createNativeQuery( "INSERT INTO HIB_TEST_DETAIL VALUES (1, 'TEST')" ).executeUpdate();
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select m from Master m where m.id = :id", Master.class )
							.setParameter( "id", 1L )
							.getResultList();
				}
		);
	}

	@AfterAll
	public void dropTestDb(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeQuery( "DROP TABLE HIB_TEST_MASTER cascade constraints" ).executeUpdate();
					session.createNativeQuery( "DROP TABLE HIB_TEST_DETAIL cascade constraints" ).executeUpdate();

				}
		);
	}

	@Entity(name = "Master")
	@Table(name = "HIB_TEST_MASTER")
	public static class Master {

		@Id
		@Column(name = "ID", precision = 10)
		private Long id;

		@Column(name = "BINARY_DATA")
		private byte[] binaryData;

		@Column(name = "NUMBER_COLUMN", precision = 10)
		private Long numberColumn;


		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ID_DETAIL", nullable = false, insertable = false, updatable = false)
		private Detail detail;

		public Long getId() {
			return id;
		}

		public byte[] getBinaryData() {
			return binaryData;
		}

	}

	@Entity
	@Table(name = "HIB_TEST_DETAIL")
	public static class Detail {

		@Id
		@Column(name = "ID_DETAIL", precision = 10)
		private Long id;

		@Column(name = "NAME", length = 40)
		private String name;

		public Long getId() {
			return this.id;
		}

		public void setId(Long idDetail) {
			this.id = idDetail;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
