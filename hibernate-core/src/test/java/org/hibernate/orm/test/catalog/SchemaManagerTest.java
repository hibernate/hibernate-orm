/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = {SchemaManagerTest.Book.class, SchemaManagerTest.Author.class})
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.DEFAULT_SCHEMA, value = "schema_manager_test"))
@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle tests run in the DBO schema")
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsTruncateTable.class)
public class SchemaManagerTest {

	@BeforeEach
	public void clean(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().dropMappedObjects(true);
	}

	private Long countBooks(SessionImplementor s) {
		return s.createQuery("select count(*) from BookForTesting", Long.class).getSingleResult();
	}

	@Test public void test0(SessionFactoryScope scope) {
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
	}

	@Entity(name="BookForTesting")
	static class Book {
		@Id String isbn = "xyz123";
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

