/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;
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
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testEntitySaving() {
		sessionFactoryScope().inTransaction(
				session -> session.createQuery( "delete SimpleEntity" ).executeUpdate()
		);

		final SimpleEntity entity = new SimpleEntity();
		entity.setId( 1 );
		entity.setSomeString( "hi" );
		entity.setSomeInteger( 2 );
		sessionFactoryScope().inTransaction( session -> session.save( entity ) );
		sessionFactoryScope().inTransaction(
				session -> {
					final String value = session.createQuery( "select s.someString from SimpleEntity s", String.class ).uniqueResult();
					assert "hi".equals( value );
				}
		);
		sessionFactoryScope().inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 1 );
					assert loaded != null;
					assert "hi".equals( loaded.getSomeString() );
				}
		);
		sessionFactoryScope().inTransaction(
				session -> {
					final List<SimpleEntity> list = session.byMultipleIds( SimpleEntity.class )
							.multiLoad( 1, 2 );
					assert list.size() == 1;
					final SimpleEntity loaded = list.get( 0 );
					assert loaded != null;
					assert "hi".equals( loaded.getSomeString() );
				}
		);
		sessionFactoryScope().inTransaction(
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
		sessionFactoryScope().inTransaction( session -> session.save( entity ) );

		sessionFactoryScope().inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getSomeString(), is( "hello world" ) );
					assertThat( loaded.getSomeLong(), is( 10L ) );
				}
		);

		sessionFactoryScope().inTransaction( session -> {
			final SimpleEntity e = session.find( SimpleEntity.class, entity.getId() );
			e.setSomeLong( 25L );
			e.setSomeString( "test" );
			session.merge( e );
		} );

		sessionFactoryScope().inTransaction(
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
		sessionFactoryScope().inTransaction( session -> session.save( entity ) );

		sessionFactoryScope().inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 3 );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getSomeString(), is( "hello world" ) );
					assertThat( loaded.getSomeLong(), is( 10L ) );
				}
		);

		sessionFactoryScope().inTransaction( session -> {
			session.remove( session.find( SimpleEntity.class, entity.getId() ) );
		} );

		sessionFactoryScope().inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 3 );
					assertThat( loaded, nullValue() );
				}
		);
	}
}
