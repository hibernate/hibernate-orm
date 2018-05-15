/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithLazyManyToOneSelfReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EntityWithLazyManyToOneSelfReferenceCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithLazyManyToOneSelfReference.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void setUp() {
		final EntityWithLazyManyToOneSelfReference entity1 = new EntityWithLazyManyToOneSelfReference(
				1,
				"first",
				Integer.MAX_VALUE
		);
		final EntityWithLazyManyToOneSelfReference entity2 = new EntityWithLazyManyToOneSelfReference(
				2,
				"second",
				Integer.MIN_VALUE,
				entity1
		);

		sessionFactoryScope().inTransaction( session -> {
			session.save( entity1 );
			session.save( entity2 );
		} );
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithLazyManyToOneSelfReference loaded = session.get(
							EntityWithLazyManyToOneSelfReference.class,
							2
					);
					session.remove( loaded.getOther() );
					session.remove( loaded );
				}
		);
	}

	@Test
	public void testGetEntityWithNoAssociation() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithLazyManyToOneSelfReference loaded = session.get(
							EntityWithLazyManyToOneSelfReference.class,
							1
					);
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), equalTo( "first" ) );
					assertThat( loaded.getOther(), nullValue() );
				}
		);
	}

	@Test
	public void testGetEntityWithTheAssociation() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithLazyManyToOneSelfReference loaded = session.get(
							EntityWithLazyManyToOneSelfReference.class,
							2
					);
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), equalTo( "second" ) );
					assertThat( loaded.getOther(), notNullValue() );
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testByMultipleIds() {
		sessionFactoryScope().inTransaction(
				session -> {
					final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithLazyManyToOneSelfReference.class )
							.multiLoad( 1, 3 );
					assert list.size() == 1;
					final EntityWithLazyManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithLazyManyToOneSelfReference.class )
							.multiLoad( 2, 3 );
					assert list.size() == 1;
					final EntityWithLazyManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "second" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testHqlSelect() {
		// todo (6.0) : the restriction here uses the wrong table alias...
		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithLazyManyToOneSelfReference e where e.other.name = 'first'",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "second" ) );
				}
		);
	}

}
