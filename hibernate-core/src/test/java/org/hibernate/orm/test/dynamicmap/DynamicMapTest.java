/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dynamicmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@TestForIssue(jiraKey = "HHH-12539")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/dynamicmap/Test.hbm.xml"
)
@SessionFactory
public class DynamicMapTest {

	@Test
	public void bootstrappingTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Map item1 = new HashMap();
			item1.put( "name", "cup" );
			item1.put( "description", "abc" );
			Map entity1 = new HashMap();
			entity1.put( "name", "first_entity" );
			item1.put( "entity", entity1 );
			session.save( "Entity1", entity1 );
			session.save( "Item1", item1 );
		} );

		scope.inTransaction( session -> {
			List result = session.createQuery( "from Item1" ).list();
			assertThat( result.size(), is( 1 ) );
			Map item1 = (Map) result.get( 0 );
			assertThat( item1.get( "name" ), is( "cup" ) );
			Object entity1 = item1.get( "entity" );
			assertThat( entity1, notNullValue() );
			assertThat( ( (Map) entity1 ).get( "name" ), is( "first_entity" ) );
		} );
	}
}

