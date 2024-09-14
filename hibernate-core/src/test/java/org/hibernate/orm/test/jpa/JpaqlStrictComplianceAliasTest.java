/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = JpaqlStrictComplianceAliasTest.Part.class
)
@ServiceRegistry(
		settings = @Setting(name = Environment.JPAQL_STRICT_COMPLIANCE, value = "true")
)
@SessionFactory
public class JpaqlStrictComplianceAliasTest {

	@Test
	public void testAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "select p.stockNumber as stockNo FROM Part p ORDER BY stockNo" )
								.getResultList()
		);
	}

	@Test
	public void testAlias2(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "select p.stockNumber as stockNo FROM Part P ORDER BY stockNo" )
								.getResultList()
		);
	}

	@Test
	public void testAlias3(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "select P.stockNumber as stockNo FROM Part P ORDER BY stockNo" )
								.getResultList()
		);
	}


	@Entity(name = "Part")
	public static class Part {
		@Id
		private Long id;
		private String name;
		private String stockNumber;

		public Part() {
		}

		public Long getId() {
			return id;
		}

		private void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getStockNumber() {
			return stockNumber;
		}

		public void setStockNumber(String stockNumber) {
			this.stockNumber = stockNumber;
		}

	}
}
