/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemamanager;

import jakarta.persistence.SchemaValidationException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.tool.schema.spi.SchemaManagementException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_SCHEMA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = {SchemaManagerExplicitSchemaTest.Book.class, SchemaManagerExplicitSchemaTest.Author.class})
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = @Setting(name = DEFAULT_SCHEMA, value = "schema_manager_test"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTruncateTable.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
public class SchemaManagerExplicitSchemaTest {

	@BeforeEach
	public void clean(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().dropMappedObjects(true);
	}

	private Long countBooks(SessionImplementor s) {
		return s.createQuery("select count(*) from BookForTesting", Long.class).getSingleResult();
	}

	@Test public void testExportValidateTruncateDrop(SessionFactoryScope scope) {
		SessionFactoryImplementor factory = scope.getSessionFactory();
		factory.getSchemaManager().exportMappedObjects(true);
		scope.inTransaction( s -> s.persist( new Book() ) );
		factory.getSchemaManager().validateMappedObjects();
		scope.inTransaction( s -> assertEquals( 1, countBooks(s) ) );
		factory.getSchemaManager().truncateMappedObjects();
		scope.inTransaction( s -> assertEquals( 0, countBooks(s) ) );
		factory.getSchemaManager().dropMappedObjects(true);
		try {
			factory.getSchemaManager().validateMappedObjects();
			fail();
		}
		catch (SchemaManagementException e) {
			assertTrue( e.getMessage().contains("ForTesting") );
		}
		try {
			factory.getSchemaManager().validate();
			fail();
		}
		catch (SchemaValidationException e) {
			assertTrue( e.getMessage().contains("ForTesting") );
		}
	}

	@Entity(name="BookForTesting")
	static class Book {
		@Id String isbn = "9781932394153";
		String title = "Hibernate in Action";
		@ManyToOne(cascade = CascadeType.PERSIST)
		Author author = new Author();
		{
			author.favoriteBook = this;
		}
	}

	@Entity(name="AuthorForTesting")
	static class Author {
		@Id String name = "Christian & Gavin";
		@ManyToOne
		public Book favoriteBook;
	}
}
