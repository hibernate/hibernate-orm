/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.identity.hhh9983;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-9983")
@RequiresDialect( OracleDialect.class )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
@DomainModel( annotatedClasses = SaveEntityTest.Company.class )
@SessionFactory
public class SaveEntityTest {
	@Test
	public void testSave(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					s.persist( new Company() );
				}
		);
	}

	@Entity
	@Table(name = "Company")
	public static class Company {
		private Integer id;
		private String name;

		public Company() {
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
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

	}
}
