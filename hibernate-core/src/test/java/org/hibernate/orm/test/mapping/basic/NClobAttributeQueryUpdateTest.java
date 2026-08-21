/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.sql.NClob;
import java.sql.SQLException;

import jakarta.persistence.Query;
import org.hibernate.engine.jdbc.proxy.NClobProxy;

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
				NClobAttributeQueryUpdateTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-20318")
public class NClobAttributeQueryUpdateTest {

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
							getLobHelper().createNClob( INITIAL_TEXT )
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
	public void testUpdateUsingNClobProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			NClob nClobValue1 = NClobProxy.generateProxy( UPDATED_TEXT_1 );
			testEntity.setNClobValue( nClobValue1 );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			checkNClobValue( testEntity, UPDATED_TEXT_1 );
		} );

		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"UPDATE TestEntity n SET n.nClobValue = :nClobValue WHERE n.id = :id"
					);
					query.setParameter( "id", 1 );
					NClob value = NClobProxy.generateProxy( UPDATED_TEXT_2 );
					query.setParameter( "nClobValue", value );
					query.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					checkNClobValue( testEntity, UPDATED_TEXT_2 );
				}
		);
	}

	@Test
	public void testUpdateUsingLobHelper(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			NClob nClobValue1 = getLobHelper().createNClob( UPDATED_TEXT_1 );
			testEntity.setNClobValue( nClobValue1 );
		} );

		scope.inTransaction( session -> {
			TestEntity testEntity = session.find( TestEntity.class, 1 );
			checkNClobValue( testEntity, UPDATED_TEXT_1 );
		} );


		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"UPDATE TestEntity n SET n.nClobValue = :nClobValue WHERE n.id = :id"
					);
					query.setParameter( "id", 1 );
					NClob value = getLobHelper().createNClob( UPDATED_TEXT_2 );
					query.setParameter( "nClobValue", value );
					query.executeUpdate();
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1 );
					checkNClobValue( testEntity, UPDATED_TEXT_2 );
				}
		);
	}

	@Test
	public void testUpdateUsingLobHelperFromReader(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.createQuery(
							"UPDATE TestEntity n SET n.nClobValue = :nClobValue WHERE n.id = :id"
					);
					query.setParameter( "id", 1 );
					try ( Reader reader = new StringReader( UPDATED_TEXT_2 ) ) {
						NClob value = getLobHelper().createNClob( reader, UPDATED_TEXT_2.length() );
						query.setParameter( "nClobValue", value );
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
					checkNClobValue( testEntity, UPDATED_TEXT_2 );
				}
		);
	}

	private static void checkNClobValue(TestEntity testEntity, String expectedValue) {
		try ( Reader reader = testEntity.getNClobValue().getCharacterStream();
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
		private NClob nClobValue;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name, NClob nClobValue) {
			this.id = id;
			this.name = name;
			this.nClobValue = nClobValue;
		}

		public Integer getId() {
			return id;
		}

		public NClob getNClobValue() {
			return nClobValue;
		}

		public void setNClobValue(NClob nClobValue) {
			this.nClobValue = nClobValue;
		}
	}
}
