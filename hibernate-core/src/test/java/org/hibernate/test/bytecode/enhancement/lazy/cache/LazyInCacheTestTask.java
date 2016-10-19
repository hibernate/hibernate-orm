/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.cache;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Barreiro
 */
public class LazyInCacheTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Order.class, Product.class, Tag.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		prepare( cfg );
	}

	public void execute() {
		EntityManager entityManager = getFactory().createEntityManager();
		Order order = new Order();
		Product product = new Product();
		order.products.add( product );
		order.data = "some data".getBytes();
		entityManager.getTransaction().begin();
		entityManager.persist( product );
		entityManager.persist( order );
		entityManager.getTransaction().commit();

		long orderId = order.id;

		entityManager = getFactory().createEntityManager();
		order = entityManager.find( Order.class, orderId );
		Assert.assertEquals( 1, order.products.size() );
		entityManager.close();

	}

	protected void cleanup() {
	}

	@Entity
	@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE )
	public static class Order {

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		long id;

		@OneToMany
		List<Product> products = new ArrayList<>();

		@OneToMany
		List<Tag> tags = new ArrayList<>();

		@Basic( fetch = FetchType.LAZY )
		@Column
		@Type( type = "org.hibernate.type.BinaryType" )
		private byte[] data;

	}

	@Entity
	public static class Product {

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		long id;

		String name;

	}

	@Entity
	public class Tag {

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		long id;

		String name;

	}
}
