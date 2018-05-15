/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import java.util.Calendar;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithOneToMany;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.hibernate.testing.junit5.FailureExpected;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Andrea Boriero
 */
@FailureExpected( value= "Persist and find og OneToMany has not yet been implemented" )
public class EntityWithOneToManyCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithOneToMany.class );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testSave() {
		EntityWithOneToMany entity = new EntityWithOneToMany( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.addOther( other );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( other );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					session.save( entity );
				} );

		sessionFactoryScope().inTransaction(
				session -> {
					EntityWithOneToMany retrieved = session.get( EntityWithOneToMany.class, 1 );
					assertThat( retrieved, notNullValue() );
				} );

	}
}
