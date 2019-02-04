/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import java.util.List;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SimpleEntityCrudTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				SimpleEntity.class,
		};
	}

	@Test
	public void testEntitySaving() {
		inTransaction(
				session -> {session.createQuery( "delete SimpleEntity" ).executeUpdate();}
		);

		final SimpleEntity entity = new SimpleEntity();
		entity.setId( 1 );
		entity.setSomeString( "hi" );
		entity.setSomeInteger( 2 );
		inTransaction( session -> {session.save( entity );} );
		inTransaction(
				session -> {
					final String value = session.createQuery( "select s.someString from SimpleEntity s", String.class ).uniqueResult();
					assert "hi".equals( value );
				}
		);
		inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 1 );
					assert loaded != null;
					assert "hi".equals( loaded.getSomeString() );
				}
		);
		inTransaction(
				session -> {
					final List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class )
							.multiLoad( 1, 2 );
					assert list.size() == 1;
					final SimpleEntity loaded = list.get( 0 );
					assert loaded != null;
					assert "hi".equals( loaded.getSomeString() );
				}
		);
		inTransaction(
				session -> {
					final SimpleEntity loaded = session.bySimpleNaturalId( SimpleEntity.class )
							.load( 2 );
					assert loaded != null;
					assert "hi".equals( loaded.getSomeString() );
				}
		);
	}

	@Test
	public void testEntityUpdate() {
		final SimpleEntity entity = new SimpleEntity();
		entity.setId( 2 );
		entity.setSomeString( "hello world" );
		entity.setSomeInteger( 5 );
		entity.setSomeLong( 10L );
		inTransaction( session -> {
			session.save( entity );
		} );

		inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getSomeString(), is( "hello world" ) );
					assertThat( loaded.getSomeLong(), is( 10L ) );
				}
		);

		inTransaction( session -> {
			final SimpleEntity e = session.find( SimpleEntity.class, entity.getId() );
			e.setSomeLong( 25L );
			e.setSomeString( "test" );
			session.merge( e );
		} );

		inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getSomeString(), is( "test" ) );
					assertThat( loaded.getSomeLong(), is( 25L ) );
				}
		);
	}

	@Test
	public void testEntityDelete() {
		final SimpleEntity entity = new SimpleEntity();
		entity.setId( 3 );
		entity.setSomeString( "hello world" );
		entity.setSomeInteger( 5 );
		entity.setSomeLong( 10L );
		inTransaction( session -> {
			session.save( entity );
		} );

		inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 3 );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getSomeString(), is( "hello world" ) );
					assertThat( loaded.getSomeLong(), is( 10L ) );
				}
		);

		inTransaction( session -> {
			session.remove( session.find( SimpleEntity.class, entity.getId() ) );
		} );

		inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 3 );
					assertThat( loaded, nullValue() );
				}
		);
	}
}
