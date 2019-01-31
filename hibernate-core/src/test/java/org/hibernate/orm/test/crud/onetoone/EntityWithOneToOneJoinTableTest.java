/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.onetoone;

import java.util.Calendar;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOneJoinTable;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToOneJoinTableTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithOneToOneJoinTable.class,
				SimpleEntity.class
		};
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session -> {
					final EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 1 );
					session.delete( loaded );
					session.delete( loaded.getOther() );
				}
		);
	}

	@Test
	public void testOperations() {
		EntityWithOneToOneJoinTable entity = new EntityWithOneToOneJoinTable( 1, "first", Integer.MAX_VALUE );

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
					final EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 1 );
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

		inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOneJoinTable e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}

	@Test
	public void testUpdate() {
		EntityWithOneToOneJoinTable entity = new EntityWithOneToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		inTransaction(
				session -> {
					session.save( other );
					session.save( entity );
				}
		);

		SimpleEntity anOther = new SimpleEntity(
				3,
				Calendar.getInstance().getTime(),
				null,
				Integer.MIN_VALUE,
				Long.MAX_VALUE,
				null
		);

		inTransaction(
				session -> {
					EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 1 );
					session.save( anOther );
					loaded.setOther( anOther );
				}
		);

		inTransaction(
				session -> {
					EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 1 );
					SimpleEntity loadedOther = loaded.getOther();
					assertThat( loadedOther, notNullValue() );
					assertThat( loadedOther.getId(), equalTo( 3 ) );
				}
		);
	}
}
