/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.SQLException;

import jakarta.persistence.Query;
import org.hibernate.engine.jdbc.proxy.ClobProxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hibernate.Hibernate.getLobHelper;

@DomainModel(
		annotatedClasses = {
				ClobAttributeQueryUpdateTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-20318")
public class ClobAttributeQueryUpdateTest {

	private static final String INITIAL_TEXT = "initial";
	private static final String UPDATED_TEXT_1 = "update1";
	private static final String UPDATED_TEXT_2 = "update2";

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity(
							1,
							"test",
							getLobHelper().createClob( INITIAL_TEXT )
					);
					session.persist( testEntity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdateUsingClobProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			Clob clobValue1 = ClobProxy.generateProxy( UPDATED_TEXT_1 );
			testEntity.setClobValue( clobValue1 );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			checkClobValue( testEntity, UPDATED_TEXT_1 );
		} );

		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"UPDATE TestEntity c SET c.clobValue = :clobValue WHERE c.id = :id"
					);
					query.setParameter( "id", 1 );
					Clob value = ClobProxy.generateProxy( UPDATED_TEXT_2 );
					query.setParameter( "clobValue", value );
					query.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					checkClobValue( testEntity, UPDATED_TEXT_2 );
				}
		);
	}

	@Test
	public void testUpdateUsingLobHelper(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			Clob clobValue1 = getLobHelper().createClob( UPDATED_TEXT_1 );
			testEntity.setClobValue( clobValue1 );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			checkClobValue( testEntity, UPDATED_TEXT_1 );
		} );


		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"UPDATE TestEntity c SET c.clobValue = :clobValue WHERE c.id = :id"
					);
					query.setParameter( "id", 1 );
					Clob value = getLobHelper().createClob( UPDATED_TEXT_2 );
					query.setParameter( "clobValue", value );
					query.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					checkClobValue( testEntity, UPDATED_TEXT_2 );
				}
		);
	}

	@Test
	public void testUpdateUsingLobHelperFromReader(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"UPDATE TestEntity c SET c.clobValue = :clobValue WHERE c.id = :id"
					);
					query.setParameter( "id", 1 );
					try ( Reader reader = new StringReader( UPDATED_TEXT_2 ) ) {
						Clob value = getLobHelper().createClob( reader, UPDATED_TEXT_2.length() );
						query.setParameter( "clobValue", value );
						query.executeUpdate();
					}
					catch ( Exception e ) {
						throw new RuntimeException( e );
					}
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					checkClobValue( testEntity, UPDATED_TEXT_2 );
				}
		);
	}

	private static void checkClobValue(TestEntity testEntity, String expectedValue) {
		try ( Reader reader = testEntity.getClobValue().getCharacterStream();
				BufferedReader bufferedReader = new BufferedReader( reader ) ) {
			assertThat( bufferedReader.readLine() ).isEqualTo( expectedValue );
		}
		catch ( SQLException | java.io.IOException e ) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		private String name;

		@Lob
		private Clob clobValue;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name, Clob clobValue) {
			this.id = id;
			this.name = name;
			this.clobValue = clobValue;
		}

		public Integer getId() {
			return id;
		}

		public Clob getClobValue() {
			return clobValue;
		}

		public void setClobValue(Clob clobValue) {
			this.clobValue = clobValue;
		}
	}
}
