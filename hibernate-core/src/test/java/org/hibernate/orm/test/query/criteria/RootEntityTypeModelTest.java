/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;


import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = { RootEntityTypeModelTest.Company.class }
)
@SessionFactory
public class RootEntityTypeModelTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( ( session ) -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<?> criteria = criteriaBuilder.createQuery();
			Root<Company> root = criteria.from( Company.class );
			assertThat( root.getModel().getId( int.class ) ).isNotNull();
		} );
	}

	@Entity
	public static class Company {

		@Id
		@GeneratedValue
		private int id;

		@Basic
		private String name;

		public Company() {
		}

		public Company(String name) {
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
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
