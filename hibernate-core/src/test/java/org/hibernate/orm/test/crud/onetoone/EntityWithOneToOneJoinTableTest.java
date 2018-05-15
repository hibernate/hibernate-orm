/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.onetoone;

import java.util.Calendar;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityWithOneToOneJoinTable;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToOneJoinTableTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithOneToOneJoinTable.class );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
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

		sessionFactoryScope().inTransaction( session -> session.save( other ) );
		sessionFactoryScope().inTransaction( session -> session.save( entity ) );

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 1 );
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

		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOneJoinTable e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}
}
