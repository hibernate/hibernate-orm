/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.cache;

import org.hibernate.Session;
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
		return new Class<?>[]{Product.class, Order.class, Tag.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		prepare( cfg );
	}

	public void execute() {
		Session entityManager = getFactory().openSession();
		Order order = new Order();
		Product product = new Product();
		order.products.add( product );
		order.data = "some data".getBytes();
		entityManager.getTransaction().begin();
		entityManager.persist( product );
		entityManager.persist( order );
		entityManager.getTransaction().commit();

		long orderId = order.id;

		entityManager = getFactory().openSession();
		order = entityManager.get( Order.class, orderId );
		Assert.assertEquals( 1, order.products.size() );
		entityManager.close();

	}

	protected void cleanup() {
	}

	@Entity(name = "Orders")
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
		@Type( type = "org.hibernate.type.BinaryType" )
		private byte[] data;

	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		long id;

		String name;

	}

	@Entity(name = "Tag")
	public class Tag {

		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		long id;

		String name;

	}
}