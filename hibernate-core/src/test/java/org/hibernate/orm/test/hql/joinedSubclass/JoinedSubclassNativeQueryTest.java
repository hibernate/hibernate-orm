/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.joinedSubclass;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jan Schatteman
 */
@DomainModel( annotatedClasses = {
		JoinedSubclassNativeQueryTest.Person.class,
		JoinedSubclassNativeQueryTest.Employee.class
} )
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class JoinedSubclassNativeQueryTest {

	@Test
	@JiraKey( value = "HHH-16180")
	public void testJoinedInheritanceNativeQuery(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();
					final String nullColumnString = sessionFactory
				.getJdbcServices()
				.getDialect()
				.getSelectClauseNullString(
									new SqlTypedMappingImpl( typeConfiguration.getBasicTypeForJavaType( String.class ) ),
									typeConfiguration
							);

		final String qry = String.format(
				Locale.ROOT,
				"select p.*, %s as company_name, 0 as clazz_  from Person p",
				nullColumnString
		);

		scope.inTransaction( (session) -> {
			// PostgreSQLDialect#getSelectClauseNullString produces e.g. `null::text` which we interpret as parameter,
			// so workaround this problem by configuring to ignore JDBC parameters
			session.setProperty( AvailableSettings.NATIVE_IGNORE_JDBC_PARAMETERS, true );

			Person p = session.createNativeQuery( qry, Person.class ).getSingleResult();
			assertThat( p ).isNotNull();
			assertThat( p.getFirstName() ).isEqualTo( "Jan" );
		} );
	}

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Person p = new Person();
			p.setFirstName( "Jan" );
			session.persist( p );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		@Basic(optional = false)
		@Column(name = "first_name")
		private String firstName;

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
	}

	@Entity(name = "Employee")
	public static class Employee extends Person {
		@Basic(optional = false)
		@Column(name = "company_name")
		private String companyName;

		public String getCompanyName() {
			return companyName;
		}

		public void setCompanyName(String companyName) {
			this.companyName = companyName;
		}
	}

}
