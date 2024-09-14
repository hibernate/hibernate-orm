/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.iterate;

import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DomainModel(
		xmlMappings = "org/hibernate/orm/test/iterate/Item.hbm.xml"
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.CACHE_REGION_PREFIX, value = "foo"),
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
public class ScrollTest {

	@Test
	public void testScroll(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Item i1 = new Item( "foo" );
					Item i2 = new Item( "bar" );
					s.persist( "Item", i1 );
					s.persist( "Item", i2 );
				}
		);

		scope.inTransaction(
				s -> {
					try (ScrollableResults sr = s.getNamedQuery( "Item.nameDesc" ).scroll()) {
						assertTrue( sr.next() );
						Item i1 = (Item) sr.get();
						assertTrue( sr.next() );
						Item i2 = (Item) sr.get();
						assertTrue( Hibernate.isInitialized( i1 ) );
						assertTrue( Hibernate.isInitialized( i2 ) );
						assertThat( i1.getName(), is( "foo" ) );
						assertThat( i2.getName(), is( "bar" ) );
						assertFalse( sr.next() );
						s.remove( i1 );
						s.remove( i2 );
					}

					assertTrue( s.getPersistenceContext().getLoadContexts().isLoadingFinished() );
				}
		);

		assertThat( scope.getSessionFactory().getStatistics().getEntityFetchCount(), is( 0L ) );
	}

}
