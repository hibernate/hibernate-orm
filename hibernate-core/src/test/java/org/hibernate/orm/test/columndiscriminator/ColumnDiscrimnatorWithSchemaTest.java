/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.columndiscriminator;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks.SupportSchemaCreation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

@DomainModel(xmlMappings = "org/hibernate/orm/test/columndiscriminator/orm.xml")
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.DEFAULT_SCHEMA, value = "GREET"),
		@Setting(name = SchemaToolingSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, value = "true")
})
@SessionFactory
@RequiresDialectFeature(feature = SupportSchemaCreation.class)
class ColumnDiscrimnatorWithSchemaTest {

	private ColumnDiscrimnatorWithSchemaTest() {
	}

	static ColumnDiscrimnatorWithSchemaTest createColumnDiscrimnatorWithSchemaTest() {
		return new ColumnDiscrimnatorWithSchemaTest();
	}

	@Test
	void testIt(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			var book = new Book( "The Art of Computer Programming",
					new SpecialBookDetails( "Hardcover", "Computer Science" ) );

			var author = new Author( "Donald Knuth", "dn@cs.com" );
			author.addBook( book );
			entityManager.persist( author );
		} );
	}
}
