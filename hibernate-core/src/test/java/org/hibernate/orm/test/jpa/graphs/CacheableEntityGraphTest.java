/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Cacheable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

public class CacheableEntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Product.class, Color.class, Tag.class };
	}

	@Test
	@JiraKey(value = "HHH-15964")
	public void test() {
		EntityManager em = getOrCreateEntityManager();

		em.getTransaction().begin();
		Tag tag = new Tag( Set.of( TagType.FOO ) );
		em.persist( tag );

		Color color = new Color();
		em.persist( color );

		Product product = new Product( tag, color );
		em.persist( product );
		em.getTransaction().commit();

		em.clear();

		EntityGraph<Product> entityGraph = em.createEntityGraph( Product.class );
		entityGraph.addAttributeNodes( "tag" );

		em.createQuery( "select p from Product p", Product.class )
				.setMaxResults( 2 )
				.setHint( "jakarta.persistence.loadgraph", entityGraph )
				.getSingleResult();
	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue
		public int id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Tag tag;

		@OneToOne(mappedBy = "product", fetch = FetchType.LAZY)
		private Color color;

		public Product() {
		}

		public Product(Tag tag, Color color) {
			this.tag = tag;
			this.color = color;
		}
	}

	@Entity(name = "Color")
	public static class Color {

		@Id
		@GeneratedValue
		public int id;

		@OneToOne(fetch = FetchType.LAZY)
		public Product product;
	}

	@Cacheable
	@Entity(name = "Tag")
	public static class Tag {

		@Id
		@GeneratedValue
		public int id;

		@Version
		public long version;

		@Enumerated(EnumType.STRING)
		@ElementCollection(fetch = FetchType.EAGER)
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		public final Set<TagType> types = new LinkedHashSet<>();

		public Tag() {
		}

		public Tag(Set<TagType> types) {
			this.types.addAll( types );
		}
	}

	public enum TagType {
		FOO,
		BAR
	}
}
