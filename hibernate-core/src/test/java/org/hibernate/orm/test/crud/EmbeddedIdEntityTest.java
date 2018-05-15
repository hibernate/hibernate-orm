/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EmbeddedIdEntity;
import org.hibernate.orm.test.support.domains.gambit.EmbeddedIdEntity.EmbeddedIdEntityId;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Chris Cranford
 */
public class EmbeddedIdEntityTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EmbeddedIdEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testEntitySaving() {
		final EmbeddedIdEntity entity = new EmbeddedIdEntity();
		final EmbeddedIdEntityId entityId = new EmbeddedIdEntityId( 25, "Acme" );
		entity.setId( entityId );
		entity.setData( "test" );

		sessionFactoryScope().inTransaction( session -> session.save( entity ) );

		// select non-embeddable data
		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery( "select e.data FROM EmbeddedIdEntity e", String.class ).uniqueResult();
					assertThat( value, is( "test" ) );
				}
		);

		// select entity
		sessionFactoryScope().inTransaction(
				session -> {
					final EmbeddedIdEntity loaded = session.createQuery( "select e FROM EmbeddedIdEntity e", EmbeddedIdEntity.class ).uniqueResult();
					assertThat( loaded.getData(), is( "test" ) );
					assertThat( loaded.getId(), equalTo( entityId ) );
				}
		);

		// select just embeddable
		sessionFactoryScope().inTransaction(
				session -> {
					final EmbeddedIdEntityId value = session.createQuery( "select e.id FROM EmbeddedIdEntity e", EmbeddedIdEntityId.class ).uniqueResult();
					assertThat( value, equalTo( entityId ) );
				}
		);

		// load entity
		sessionFactoryScope().inTransaction(
				session -> {
					final EmbeddedIdEntity loaded = session.get( EmbeddedIdEntity.class, entityId );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getId(), notNullValue() );
					assertThat( loaded.getId(), equalTo( entityId ) );
					assertThat( loaded.getData(), is( "test" ) );
				}
		);
	}
}
