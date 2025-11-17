/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.StoredProcedureParameter;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.test.c3p0.util.GradleParallelTestingC3P0ConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(OracleDialect.class)
@JiraKey(value = "HHH-10256")
@ServiceRegistry(settingProviders = @SettingProvider( settingName = CONNECTION_PROVIDER, provider = GradleParallelTestingC3P0ConnectionProvider.SettingProviderImpl.class ))
@DomainModel(annotatedClasses = OracleSQLCallableStatementProxyTest.Person.class)
@SessionFactory
public class OracleSQLCallableStatementProxyTest {
	@BeforeEach
	void prepareDatabase(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.doWork( (connection) -> {
				try (Statement statement =  connection.createStatement()) {
					statement.executeUpdate(
							"""
									CREATE OR REPLACE FUNCTION fn_person ( \
									personId IN NUMBER) \
										RETURN SYS_REFCURSOR \
									IS \
										persons SYS_REFCURSOR; \
									BEGIN \
									OPEN persons FOR \
											SELECT \
												p.id AS "p.id", \
												p.name AS "p.name", \
												p.nickName AS "p.nickName" \
										FROM person p \
										WHERE p.id = personId; \
									RETURN persons; \
									END;"""
					);
				}
			} );

			Person person1 = new Person();
			person1.setId( 1L );
			person1.setName( "John Doe" );
			person1.setNickName( "JD" );
			session.persist( person1 );
		} );
	}

	@AfterEach
	void cleanupDatabase(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testStoredProcedureOutParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			//noinspection unchecked
			List<Object[]> persons = session
					.createNamedStoredProcedureQuery( "getPerson" )
					.setParameter(1, 1L)
					.getResultList();
			assertThat( persons ).hasSize( 1 );
		} );
	}

	@NamedStoredProcedureQuery(
			name = "getPerson",
			procedureName = "fn_person",
			resultSetMappings = "person",
			hints = @QueryHint(name = "org.hibernate.callableFunction", value = "true"),
			parameters = @StoredProcedureParameter(type = Long.class)
	)
	@SqlResultSetMappings({
		@SqlResultSetMapping(
			name = "person",
			entities = {
				@EntityResult(
						entityClass = Person.class,
						fields = {
								@FieldResult( name = "id", column = "p.id" ),
								@FieldResult( name = "name", column = "p.name" ),
								@FieldResult( name = "nickName", column = "p.nickName" ),
						}
				)
			}
		),
	})
	@Entity(name = "Person")
	@jakarta.persistence.Table(name = "person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		private String nickName;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}
	}
}
