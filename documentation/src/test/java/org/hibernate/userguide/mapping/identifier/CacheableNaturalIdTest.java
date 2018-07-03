/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.identifier;

import java.util.Map;
import javax.cache.configuration.MutableConfiguration;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cache.jcache.JCacheHelper;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CacheableNaturalIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public void buildEntityManagerFactory() {
		JCacheHelper.locateStandardCacheManager().createCache( "default-update-timestamps-region", new MutableConfiguration<>() );
		JCacheHelper.locateStandardCacheManager().createCache( "default-query-results-region", new MutableConfiguration<>() );
		JCacheHelper.locateStandardCacheManager().createCache( "org.hibernate.userguide.mapping.identifier.CacheableNaturalIdTest$Book##NaturalId", new MutableConfiguration<>() );
//		JCacheHelper.locateStandardCacheManager().createCache( "", new MutableConfiguration<>() );

		super.buildEntityManagerFactory();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class
		};
	}

	@Override
	@SuppressWarnings( "unchecked" )
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.TRUE.toString() );
		options.put( AvailableSettings.CACHE_REGION_FACTORY, "jcache" );
		options.put( AvailableSettings.USE_QUERY_CACHE, Boolean.TRUE.toString() );
		options.put( AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString() );
		options.put( AvailableSettings.CACHE_REGION_PREFIX, "" );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Book book = new Book();
			book.setId( 1L );
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );
			book.setIsbn( "978-9730228236" );

			entityManager.persist( book );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::naturalid-cacheable-load-access-example[]
			Book book = entityManager
				.unwrap(Session.class)
				.bySimpleNaturalId( Book.class )
				.load( "978-9730228236" );
			//end::naturalid-cacheable-load-access-example[]

			assertEquals("High-Performance Java Persistence", book.getTitle());
		} );
	}

	//tag::naturalid-cacheable-mapping-example[]
	@Entity(name = "Book")
	@NaturalIdCache
	public static class Book {

		@Id
		private Long id;

		private String title;

		private String author;

		@NaturalId
		private String isbn;

		//Getters and setters are omitted for brevity
	//end::naturalid-cacheable-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getAuthor() {
			return author;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}
	//tag::naturalid-cacheable-mapping-example[]
	}
	//end::naturalid-cacheable-mapping-example[]
}
