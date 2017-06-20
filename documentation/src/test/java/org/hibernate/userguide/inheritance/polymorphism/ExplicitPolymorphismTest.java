/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.inheritance.polymorphism;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class ExplicitPolymorphismTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Book.class,
			Blog.class,
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::entity-inheritance-polymorphism-persist-example[]
			Book book = new Book();
			book.setId( 1L );
			book.setAuthor( "Vlad Mihalcea" );
			book.setTitle( "High-Performance Java Persistence" );
			entityManager.persist( book );

			Blog blog = new Blog();
			blog.setId( 1L );
			blog.setSite( "vladmihalcea.com" );
			entityManager.persist( blog );
			//end::entity-inheritance-polymorphism-persist-example[]
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::entity-inheritance-polymorphism-fetch-example[]
			List<DomainModelEntity> accounts = entityManager
			.createQuery(
				"select e " +
				"from org.hibernate.userguide.inheritance.polymorphism.DomainModelEntity e" )
			.getResultList();

			assertEquals(1, accounts.size());
			assertTrue( accounts.get( 0 ) instanceof Book );
			//end::entity-inheritance-polymorphism-fetch-example[]
		} );
	}


	//tag::entity-inheritance-polymorphism-mapping-example[]
	@Entity(name = "Event")
	public static class Book implements DomainModelEntity<Long> {

		@Id
		private Long id;

		@Version
		private Integer version;

		private String title;

		private String author;

		//Getter and setters omitted for brevity
		//end::entity-inheritance-polymorphism-mapping-example[]

		@Override
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public Integer getVersion() {
			return version;
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
	//tag::entity-inheritance-polymorphism-mapping-example[]
	}

	@Entity(name = "Blog")
	@Polymorphism(type = PolymorphismType.EXPLICIT)
	public static class Blog implements DomainModelEntity<Long> {

		@Id
		private Long id;

		@Version
		private Integer version;

		private String site;

		//Getter and setters omitted for brevity
		//end::entity-inheritance-polymorphism-mapping-example[]

		@Override
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Override
		public Integer getVersion() {
			return version;
		}

		public String getSite() {
			return site;
		}

		public void setSite(String site) {
			this.site = site;
		}
	//tag::entity-inheritance-polymorphism-mapping-example[]
	}
	//end::entity-inheritance-polymorphism-mapping-example[]
}
