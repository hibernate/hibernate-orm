/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.schema;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
public class SchemaGenerationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
			Book.class,
			Customer.class
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		if ( getDialect().getClass().equals( H2Dialect.class ) ) {
			settings.put(
					AvailableSettings.HBM2DDL_IMPORT_FILES,
					"schema-generation.sql"
			);
			settings.put( org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, "update" );
		}
		return settings;
	}

	@Override
	protected String[] getMappings() {
		if ( PostgreSQL81Dialect.class.isAssignableFrom( getDialect().getClass() ) ) {
			return new String[] { "org/hibernate/userguide/schema/SchemaGenerationTest.hbm.xml" };
		}
		return super.getMappings();
	}

	@Test
	@RequiresDialect( H2Dialect.class )
	public void testH2() {
	}

	@Test
	@RequiresDialect( PostgreSQL81Dialect.class )
	public void testPostgres() {
	}

	//tag::schema-generation-domain-model-example[]
	@Entity(name = "Customer")
	public class Customer {

		@Id
		private Integer id;

		private String name;

		@Basic( fetch = FetchType.LAZY )
		private UUID accountsPayableXrefId;

		@Lob
		@Basic( fetch = FetchType.LAZY )
		@LazyGroup( "lobs" )
		private Blob image;

		//Getters and setters are omitted for brevity

	//end::schema-generation-domain-model-example[]

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public UUID getAccountsPayableXrefId() {
			return accountsPayableXrefId;
		}

		public void setAccountsPayableXrefId(UUID accountsPayableXrefId) {
			this.accountsPayableXrefId = accountsPayableXrefId;
		}

		public Blob getImage() {
			return image;
		}

		public void setImage(Blob image) {
			this.image = image;
		}
	//tag::schema-generation-domain-model-example[]
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "author")
		private List<Book> books = new ArrayList<>();

		//Getters and setters are omitted for brevity

	//end::schema-generation-domain-model-example[]

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

		public List<Book> getBooks() {
			return books;
		}
	//tag::schema-generation-domain-model-example[]
	}

	@Entity(name = "Book")
	public static class Book {

		@Id
		private Long id;

		private String title;

		@NaturalId
		private String isbn;

		@ManyToOne
		private Person author;

		//Getters and setters are omitted for brevity

	//end::schema-generation-domain-model-example[]

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

		public Person getAuthor() {
			return author;
		}

		public void setAuthor(Person author) {
			this.author = author;
		}

		public String getIsbn() {
			return isbn;
		}

		public void setIsbn(String isbn) {
			this.isbn = isbn;
		}
	//tag::schema-generation-domain-model-example[]
	}
	//end::schema-generation-domain-model-example[]
}
