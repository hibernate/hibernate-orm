/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.onetomany;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithOneToMany;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToManyCrudTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithOneToMany.class,
				SimpleEntity.class
		};
	}

	@Test
	public void testSave() {
		EntityWithOneToMany entity = new EntityWithOneToMany( 1, "first", Integer.MAX_VALUE );

		SimpleEntity firstOther = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);
		SimpleEntity secondOther = new SimpleEntity(
				3,
				Calendar.getInstance().getTime(),
				null,
				Integer.MIN_VALUE,
				Long.MIN_VALUE,
				null
		);

		entity.addOther( firstOther );
		entity.addOther( secondOther );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( firstOther );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( secondOther );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( entity );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToMany retrieved = session.get( EntityWithOneToMany.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<SimpleEntity> others = retrieved.getOthers();
					assertFalse(
							Hibernate.isInitialized( others ),
							"The association should ne not initialized"

					);
					assertThat( others.size(), is( 2 ) );

					Map<Integer, SimpleEntity> othersById = new HashMap<>();
					for ( SimpleEntity simpleEntity : others ) {
						othersById.put( simpleEntity.getId(), simpleEntity );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 2 ).getSomeLong(), is( Long.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeLong(), is( Long.MIN_VALUE ) );

				}
		);
	}
}
