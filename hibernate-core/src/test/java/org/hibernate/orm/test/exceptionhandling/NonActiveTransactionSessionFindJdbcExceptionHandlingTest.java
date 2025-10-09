/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import org.hibernate.JDBCException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.hibernate.cfg.JpaComplianceSettings.JPA_TRANSACTION_COMPLIANCE;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-13737")
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = NonActiveTransactionSessionFindJdbcExceptionHandlingTest.AnEntity.class,
		integrationSettings = @Setting(name = JPA_TRANSACTION_COMPLIANCE, value = "true")
)
public class NonActiveTransactionSessionFindJdbcExceptionHandlingTest {

	@Test
	public void testJdbcExceptionThrown(EntityManagerFactoryScope factoryScope) {
		// delete "description" column so that a JDBCException caused by a SQLException is thrown when looking up the AnEntity
		factoryScope.inTransaction( (entityManager) -> {
			entityManager.createNativeQuery( "alter table AnEntity drop column description" ).executeUpdate();
		} );

		factoryScope.inTransaction( (entityManager) -> {
			try {
				entityManager.find( AnEntity.class, 1 );
				fail( "A PersistenceException should have been thrown." );
			}
			catch ( PersistenceException ex ) {
				assertInstanceOf( JDBCException.class, ex );
				assertInstanceOf( SQLException.class, ex.getCause() );
			}
		} );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	public void setupData(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			entityManager.persist( new AnEntity( 1, "description" ) );
		} );
	}

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;
		@Column(name = "description")
		private String description;

		AnEntity() {
		}

		AnEntity(int id, String description) {
			this.id = id;
			this.description = description;
		}
	}
}
