/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dynamicmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.InstanceOfAssertFactories;

@TestForIssue(jiraKey = "HHH-16100")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/dynamicmap/onetoone.hbm.xml"
)
@SessionFactory
public class DynamicMapOneToOneTest {

	@Test
	void test(SessionFactoryScope scope) {
		String bookTypeName = "Book";
		String quoteTypeName = "Quote";

		scope.inTransaction( session -> {
			Map<String, Object> book = new HashMap<>();
			book.put( "id", 1 );
			book.put( "title", "Hyperion" );

			Map<String, Object> quote1 = new HashMap<>();
			quote1.put( "id", 2 );
			quote1.put( "author", "The New York Times Book Review" );
			quote1.put( "content", "An unfailingly inventive narrative" );

			book.put( "quote", quote1 );
			quote1.put( "book", book );

			session.persist( bookTypeName, book );
			session.persist( quoteTypeName, quote1 );
		} );

		scope.inTransaction( session -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> book = (Map<String, Object>) session.get( bookTypeName, 1 );
			assertThat( book )
					.contains(
							entry( "id", 1 ),
							entry( "title", "Hyperion" )
					)
					.extractingByKey( "quote", InstanceOfAssertFactories.map( String.class, Object.class ) )
					.contains(
							entry( "id", 2 ),
							entry( "author", "The New York Times Book Review" ),
							entry( "content", "An unfailingly inventive narrative" )
					);
		} );
	}
}
