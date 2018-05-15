/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import java.util.Calendar;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithLazyOneToOne;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class EntityWithLazyOneToOneTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithLazyOneToOne.class );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void setUp() {
		EntityWithLazyOneToOne entity = new EntityWithLazyOneToOne( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		sessionFactoryScope().inTransaction( session -> {
			session.save( other );
			session.save( entity );
		} );
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction( session -> {
			deleteAll();
		} );
	}

	@Test
	public void testGet() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.get( EntityWithLazyOneToOne.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assertFalse(
							"The lazy association should not be initialized",
							Hibernate.isInitialized( loaded.getOther() )
					);

					SimpleEntity loadedOther = loaded.getOther();
					assert loadedOther != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
					assertFalse(
							"getId() should not trigger the lazy association initialization",
							Hibernate.isInitialized( loaded.getOther() )
					);

					loadedOther.getSomeDate();
					assertTrue(
							"The lazy association should be initialized",
							Hibernate.isInitialized( loaded.getOther() )
					);

				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assert loaded != null;
					assertThat( loaded.getSomeInteger(), equalTo( Integer.MAX_VALUE ) );
				}
		);
	}

	@Test
	public void testHqlSelect() {

		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithLazyOneToOne e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}

	private void deleteAll() {

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.get( EntityWithLazyOneToOne.class, 1 );
					assert loaded != null;
					assert loaded.getOther() != null;
					session.remove( loaded );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithLazyOneToOne notfound = session.find( EntityWithLazyOneToOne.class, 1 );
					assertThat( notfound, CoreMatchers.nullValue() );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final SimpleEntity simpleEntity = session.find( SimpleEntity.class, 2 );
					assertThat( simpleEntity, notNullValue() );
					session.remove( simpleEntity );
				}
		);
	}
}

