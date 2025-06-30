/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.callbacks.RemoteControl;
import org.hibernate.jpa.test.callbacks.Television;
import org.hibernate.jpa.test.callbacks.VideoSystem;
import org.hibernate.jpa.test.inheritance.Fruit;
import org.hibernate.jpa.test.inheritance.Strawberry;
import org.hibernate.jpa.test.metamodel.Address;
import org.hibernate.jpa.test.metamodel.Alias;
import org.hibernate.jpa.test.metamodel.Country;
import org.hibernate.jpa.test.metamodel.CreditCard;
import org.hibernate.jpa.test.metamodel.Customer;
import org.hibernate.jpa.test.metamodel.Info;
import org.hibernate.jpa.test.metamodel.LineItem;
import org.hibernate.jpa.test.metamodel.Order;
import org.hibernate.jpa.test.metamodel.Phone;
import org.hibernate.jpa.test.metamodel.Product;
import org.hibernate.jpa.test.metamodel.ShelfLife;
import org.hibernate.jpa.test.metamodel.Spouse;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.transform.ResultTransformer;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CriteriaAliasTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				Alias.class,
				Phone.class,
				Address.class,
				Country.class,
				CreditCard.class,
				Info.class,
				Spouse.class,
				LineItem.class,
				Order.class,
				Product.class,
				ShelfLife.class,
				// @Inheritance
				Fruit.class,
				Strawberry.class,
				// @MappedSuperclass
				VideoSystem.class,
				Television.class,
				RemoteControl.class
		};
	}

	@Before
	public void setUp(){
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Customer customer = new Customer();
			customer.setId( "id" );
			customer.setName( " David R. Vincent " );
			entityManager.persist( customer );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-")
	public void testAliasJPACriteriaQuery() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = criteriaBuilder.createTupleQuery();
			final Root<Customer> from = query.from( Customer.class );
			query.multiselect(
					from.get( "id" ),
					from.get( "name" ).alias( "my.name" ),
					from.get( "age" ).alias( "alter" )
			);

			final MyResultTransformer transformer = new MyResultTransformer();
			entityManager.createQuery( query )
					.unwrap( QueryImplementor.class )
					.setResultTransformer( transformer )
					.getResultList();
			assertThat( transformer.aliases, is(notNullValue()) );
			assertThat( transformer.aliases.length, is( 3 ) );
			assertThat( transformer.aliases[0], is(nullValue()) );
			assertThat( transformer.aliases[1], is( "my.name" ) );
			assertThat( transformer.aliases[2], is( "alter" ) );
		} );
	}

	private static class MyResultTransformer implements ResultTransformer {
		String[] aliases;

		@Override
		public Object transformTuple(Object[] tuple, String[] aliases) {
			this.aliases = aliases;
			return tuple;
		}

		@Override
		public List transformList(List collection) {
			return collection;
		}
	}
}
