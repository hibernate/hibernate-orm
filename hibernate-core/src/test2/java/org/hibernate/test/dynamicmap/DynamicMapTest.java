/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dynamicmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

@TestForIssue( jiraKey = "HHH-12539")
public class DynamicMapTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] {
				"/dynamicmap/Test.hbm.xml"
		};
	}

	@Test
	public void bootstrappingTest() {
		doInHibernate( this::sessionFactory, session -> {
			Map item1 = new HashMap();
			item1.put( "name", "cup" );
			item1.put( "description", "abc" );
			Map entity1 = new HashMap();
			entity1.put( "name", "first_entity" );
			item1.put( "entity", entity1 );
			session.save( "Entity1", entity1 );
			session.save( "Item1", item1 );
		} );

		doInHibernate( this::sessionFactory, session -> {
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

