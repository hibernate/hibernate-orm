/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.manytoone;

import java.util.Calendar;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneJoinTable;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EntityWithManyToOneJoinTableTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithManyToOneJoinTable.class,
				SimpleEntity.class
		};
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );
					session.delete( loaded );
					session.delete( loaded.getOther() );
				}
		);
	}

	@Test
	public void testSave() {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		inTransaction( session -> session.save( other ) );
		inTransaction( session -> session.save( entity ) );

		inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
				}
		);

		inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assert loaded != null;
					assertThat( loaded.getSomeInteger(), equalTo( Integer.MAX_VALUE ) );
				}
		);
	}

	@Test
	public void testHqlSelect() {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		inTransaction( session -> {
			session.save( other );
			session.save( entity );
		} );

		inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithManyToOneJoinTable e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testUpdate() {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		inTransaction( session -> {
			session.save( other );
			session.save( entity );
		} );

		SimpleEntity anOther = new SimpleEntity(
				3,
				Calendar.getInstance().getTime(),
				null,
				Integer.MIN_VALUE,
				Long.MIN_VALUE,
				null
		);

		inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );
					assert loaded != null;
					session.save( anOther );
					loaded.setOther( anOther );
				}
		);

		inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );

					assertThat( loaded.getOther(), notNullValue() );
					assertThat( loaded.getOther().getId(), equalTo( 3 ) );
				}
		);
	}
}
