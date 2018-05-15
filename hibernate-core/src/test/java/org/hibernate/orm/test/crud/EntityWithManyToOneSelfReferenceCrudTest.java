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
import org.hibernate.orm.test.support.domains.gambit.EntityWithManyToOneSelfReference;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class EntityWithManyToOneSelfReferenceCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithManyToOneSelfReference.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testGetEntity() {

		final EntityWithManyToOneSelfReference entity1 = new EntityWithManyToOneSelfReference(
				1,
				"first",
				Integer.MAX_VALUE
		);

		final EntityWithManyToOneSelfReference entity2 = new EntityWithManyToOneSelfReference(
				2,
				"second",
				Integer.MAX_VALUE,
				entity1
		);

		sessionFactoryScope().inTransaction( session -> {
			session.save( entity1 );
			session.save( entity2 );
		} );

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference loaded = session.get(
							EntityWithManyToOneSelfReference.class,
							2
					);
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "second" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference loaded = session.get(
							EntityWithManyToOneSelfReference.class,
							1
					);
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assertThat( loaded.getOther(), nullValue() );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithManyToOneSelfReference.class )
							.multiLoad( 1, 3 );
					assert list.size() == 1;
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithManyToOneSelfReference.class )
							.multiLoad( 2, 3 );
					assert list.size() == 1;
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "second" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);

		// todo (6.0) : the restriction here uses the wrong table alias...
		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithManyToOneSelfReference e where e.other.name = 'first'",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "second" ) );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference queryResult = session.createQuery(
							"select e from EntityWithManyToOneSelfReference e where e.other.name = 'first'",
							EntityWithManyToOneSelfReference.class
					).uniqueResult();
					assertThat( queryResult.getName(), equalTo( "second" ) );
				}
		);
	}
}
