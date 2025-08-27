/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5" )
)
@DomainModel( annotatedClasses = OptionalSecondaryTableBatchTest.Company.class )
@SessionFactory
public class OptionalSecondaryTableBatchTest {
	@Test
	public void testManaged(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<Company> companies = session.createQuery( "from Company order by id", Company.class ).list();
			for ( int i = 0 ; i < companies.size() ; i++ ) {
				final Company company = companies.get( i );
				company.taxNumber = 2 * i;
				session.merge( company );
			}
		} );

		scope.inTransaction( (session) -> {
			final List<Company> companies = session.createQuery( "from Company order by id", Company.class ).list();
			for ( int i = 0 ; i < companies.size() ; i++ ) {
				assertThat( companies.get( i ).taxNumber ).isEqualTo( 2 * i );
			}
		} );
	}
	@Test
	public void testMerge(SessionFactoryScope scope) {
		final List<Company> companies = scope.fromTransaction( (session) -> {
			//noinspection CodeBlock2Expr
			return session.createQuery( "from Company", Company.class ).list();
		} );

		scope.inTransaction( (session) -> {
			for ( int i = 0 ; i < companies.size() ; i++ ) {
				final Company company = companies.get( i );
				company.taxNumber = 2 * i;
				session.merge( company );
			}
		} );

		scope.inTransaction( (session) -> {
			for ( int i = 0 ; i < companies.size() ; i++ ) {
				assertThat( session.get( Company.class, companies.get( i ).id ).taxNumber ).isEqualTo( 2 * i );
			}
		} );
	}


	@BeforeEach
	public void setupTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			for ( int i = 0; i < 10; i++ ) {
				final Company company = new Company( i );
				if ( i % 2 == 0 ) {
					company.taxNumber = i;
				}
				session.persist( company );
			}
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Company")
	@SecondaryTable( name = "company_tax" )
	@SecondaryRow(table = "company_tax", optional = true)
	public static class Company {

		@Id
		private int id;

		@Version
		@Column( name = "ver" )
		private int version;

		private String name;

		@Column(table = "company_tax")
		private Integer taxNumber;

		public Company() {
		}

		public Company(int id) {
			this.id = id;
		}

		public Company(int id, String name, Integer taxNumber) {
			this.id = id;
			this.name = name;
			this.taxNumber = taxNumber;
		}
	}
}
