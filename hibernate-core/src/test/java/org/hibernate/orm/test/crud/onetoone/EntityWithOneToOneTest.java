/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.onetoone;

import java.util.Calendar;

import org.hamcrest.CoreMatchers;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithOneToOne;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToOneTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithOneToOne.class );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void setUp() {
		EntityWithOneToOne entity = new EntityWithOneToOne( 1, "first", Integer.MAX_VALUE );

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
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
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
	public void testQueryParentAttribute2() {
		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOne e where e.id = 1",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testQueryParentAttribute3() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithOneToOne value = session.createQuery(
							"select e from EntityWithOneToOne e where e.id = 1",
							EntityWithOneToOne.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testQueryParentAttribute() {
		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOne e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testQueryParent() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithOneToOne value = session.createQuery(
							"select e from EntityWithOneToOne e where e.other.id = 2",
							EntityWithOneToOne.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );
				}
		);
	}

	private void deleteAll() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
					assert loaded != null;
					assert loaded.getOther() != null;
					session.remove( loaded );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithOneToOne notfound = session.find( EntityWithOneToOne.class, 1 );
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
