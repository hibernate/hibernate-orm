/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemamanager;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = {SchemaManagerLoadScriptTest.Book.class, SchemaManagerLoadScriptTest.Author.class})
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = @Setting(name = JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE,
									value = "org/hibernate/orm/test/schemamanager/data.sql"))
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsTruncateTable.class)
public class SchemaManagerLoadScriptTest {

	@BeforeEach
	public void clean(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().dropMappedObjects(true);
	}

	private Long countBooks(SessionImplementor s) {
		return s.createQuery("select count(*) from Book", Long.class).getSingleResult();
	}

	private Long countAuthors(SessionImplementor s) {
		return s.createQuery("select count(*) from Author", Long.class).getSingleResult();
	}

	@Test
	public void testExportValidateTruncateDrop(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		factory.getSchemaManager().exportMappedObjects(true);
		factory.getSchemaManager().validateMappedObjects();
		Author author = new Author(); author.name = "Steve Ebersole";
		scope.inTransaction( s -> s.persist(author) );
		scope.inTransaction( s -> assertEquals( 1, countBooks(s) ) );
		scope.inTransaction( s -> assertEquals( 3, countAuthors(s) ) );
		factory.getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> assertEquals( 2, countAuthors(s) ) );
		factory.getSchemaManager().dropMappedObjects(true);
	}

	@Test
	public void testExportPopulateValidateDrop(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		factory.getSchemaManager().exportMappedObjects(true);
		scope.inTransaction( s -> s.createMutationQuery( "delete Author" ).executeUpdate() );
		scope.inTransaction( s -> s.createMutationQuery( "delete Book" ).executeUpdate() );
		scope.inTransaction( s -> assertEquals( 0, countBooks(s) ) );
		scope.inTransaction( s -> assertEquals( 0, countAuthors(s) ) );
		factory.getSchemaManager().populate();
		scope.inTransaction( s -> assertEquals( 1, countBooks(s) ) );
		scope.inTransaction( s -> assertEquals( 2, countAuthors(s) ) );
		factory.getSchemaManager().validateMappedObjects();
		Author author = new Author(); author.name = "Steve Ebersole";
		scope.inTransaction( s -> s.persist(author) );
		scope.inTransaction( s -> assertEquals( 1, countBooks(s) ) );
		scope.inTransaction( s -> assertEquals( 3, countAuthors(s) ) );
		factory.getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> assertEquals( 2, countAuthors(s) ) );
		factory.getSchemaManager().dropMappedObjects(true);
	}

	@Entity(name="Book") @Table(name="Books")
	static class Book {
		@Id
		String isbn;
		String title;
	}

	@Entity(name="Author") @Table(name="Authors")
	static class Author {
		@Id String name;
		@ManyToMany @JoinTable(name = "BooksByAuthor")
		public Set<Book> books;
	}
}
