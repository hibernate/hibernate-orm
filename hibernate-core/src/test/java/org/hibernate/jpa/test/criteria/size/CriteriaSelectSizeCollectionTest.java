/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.size;

import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH014245")
public class CriteriaSelectSizeCollectionTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class, Alias.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
					 Customer customer = new Customer( "1", "Phil" );
					 Alias alias = new Alias( "2", "p" );
					 customer.addAlias( alias );
					 entityManager.persist( customer );
				 }
		);
	}

	@Test
	public void testSelectCollectionSize() {
		doInJPA( this::entityManagerFactory, entityManager -> {
					 CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					 CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					 Root<Customer> customer = query.from( Customer.class );

					 Expression<Integer> aliases = criteriaBuilder.size( customer.get( "aliases" ) );
					 query.select( aliases );
					 query.where( criteriaBuilder.equal( customer.get( "id" ), "1" ) );

					 TypedQuery<Integer> tq = entityManager.createQuery( query );
					 Integer size = tq.getSingleResult();
					 assertThat( size, is( 1 ) );
				 }
		);
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER_TABLE")
	public static class Customer {

		@Id
		private String id;

		private String name;

		@ManyToMany(cascade = CascadeType.ALL)
		private Collection<Alias> aliases = new ArrayList<>();

		public Customer() {
		}

		public Customer(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void addAlias(Alias alias) {
			aliases.add( alias );
		}
	}

	@Entity(name = "Alias")
	@Table(name = "ALIAS_TABLE")
	public static class Alias implements java.io.Serializable {

		@Id
		private String id;

		private String alias;

		public Alias() {
		}

		public Alias(String id, String alias) {
			this.id = id;
			this.alias = alias;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}
	}

}
