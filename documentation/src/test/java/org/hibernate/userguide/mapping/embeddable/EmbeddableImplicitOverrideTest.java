/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.embeddable;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddableImplicitOverrideTest
	extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class,
			Country.class
		};
	}

	@Override
	protected void initialize(MetadataBuilder metadataBuilder) {
		super.initialize( metadataBuilder );
		//tag::embeddable-multiple-ImplicitNamingStrategyComponentPathImpl[]
		metadataBuilder.applyImplicitNamingStrategy(
			ImplicitNamingStrategyComponentPathImpl.INSTANCE
		);
		//end::embeddable-multiple-ImplicitNamingStrategyComponentPathImpl[]
	}

	@Test
	public void testLifecycle() {
		doInHibernate( this::sessionFactory, session -> {
			Country canada = new Country();
			canada.setName( "Canada" );
			session.persist( canada );

			Country usa = new Country();
			usa.setName( "USA" );
			session.persist( usa );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Country canada = session.byNaturalId( Country.class ).using( "name", "Canada" ).load();
			Country usa = session.byNaturalId( Country.class ).using( "name", "USA" ).load();

			Book book = new Book();
			book.setTitle( "High-Performance Java Persistence" );
			book.setAuthor( "Vlad Mihalcea" );
			book.setEbookPublisher( new Publisher( "Leanpub", canada ) );
			book.setPaperBackPublisher( new Publisher( "Amazon", usa ) );

			session.persist( book );
		} );
	}

	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	@Entity(name = "Book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		private String title;

		private String author;

		private Publisher ebookPublisher;

		private Publisher paperBackPublisher;

		//Getters and setters are omitted for brevity
	//end::embeddable-multiple-namingstrategy-entity-mapping[]

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

		public Publisher getEbookPublisher() {
			return ebookPublisher;
		}

		public void setEbookPublisher(Publisher ebookPublisher) {
			this.ebookPublisher = ebookPublisher;
		}

		public Publisher getPaperBackPublisher() {
			return paperBackPublisher;
		}

		public void setPaperBackPublisher(Publisher paperBackPublisher) {
			this.paperBackPublisher = paperBackPublisher;
		}
	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	}

	@Embeddable
	public static class Publisher {

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Country country;

		//Getters and setters, equals and hashCode methods omitted for brevity
	//end::embeddable-multiple-namingstrategy-entity-mapping[]

		public Publisher(String name, Country country) {
			this.name = name;
			this.country = country;
		}

		private Publisher() {}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}
	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	}

	@Entity(name = "Country")
	public static class Country {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String name;

		//Getters and setters are omitted for brevity
	//end::embeddable-multiple-namingstrategy-entity-mapping[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	//tag::embeddable-multiple-namingstrategy-entity-mapping[]
	}
	//end::embeddable-multiple-namingstrategy-entity-mapping[]
}
