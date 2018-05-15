/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.crud;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hibernate.orm.test.jpa.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class SimpleEntityCrudTest extends EntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SimpleEntity.class };
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testEntityPersist() {
		final SimpleEntity entity = new SimpleEntity();
		entity.setId( 1 );
		entity.setSomeString( "hi" );
		entity.setSomeInteger( 2 );
		entityManagerFactoryScope().inTransaction( entityManager -> entityManager.persist( entity ) );

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final String value = entityManager.createQuery( "select s.someString from SimpleEntity s", String.class )
							.getSingleResult();
					assertThat( value, CoreMatchers.is( "hi" ) );
				}
		);

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final SimpleEntity loaded = entityManager.find( SimpleEntity.class, 1 );
					assertThat( loaded, CoreMatchers.notNullValue() );
					assertThat( loaded.getSomeString(), CoreMatchers.is( "hi" ) );
				}
		);

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final List<SimpleEntity> list = entityManager.createQuery( "FROM SimpleEntity", SimpleEntity.class ).getResultList();
					assertThat( list.size(), CoreMatchers.is( 1 ) );
					final SimpleEntity loaded = list.get( 0 );
					assertThat( loaded, CoreMatchers.notNullValue() );
					assertThat( loaded.getSomeString(), CoreMatchers.is( "hi" ) );
				}
		);
	}

	@Test
	public void testEntityMerge() {
		final SimpleEntity entity = new SimpleEntity();
		entity.setId( 2 );
		entity.setSomeString( "hello world" );
		entity.setSomeInteger( 5 );
		entity.setSomeLong( 10L );
		entityManagerFactoryScope().inTransaction( entityManager -> entityManager.persist( entity ) );

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final SimpleEntity loaded = entityManager.find( SimpleEntity.class, 2 );
					assertThat( loaded, CoreMatchers.notNullValue() );
					assertThat( loaded.getSomeString(), CoreMatchers.is( "hello world" ) );
					assertThat( loaded.getSomeLong(), CoreMatchers.is( 10L ) );
				}
		);

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final SimpleEntity loaded = entityManager.find( SimpleEntity.class, entity.getId() );
					loaded.setSomeLong( 25L );
					loaded.setSomeString( "test" );
					entityManager.merge( loaded );
				}
		);

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final SimpleEntity loaded = entityManager.find( SimpleEntity.class, entity.getId() );
					assertThat( loaded, CoreMatchers.notNullValue() );
					assertThat( loaded.getSomeString(), CoreMatchers.is( "test" ) );
					assertThat( loaded.getSomeLong(), CoreMatchers.is( 25L ) );
				}
		);
	}

	@Test
	public void testEntityRemove() {
		final SimpleEntity entity = new SimpleEntity();
		entity.setId( 3 );
		entity.setSomeString( "hello world" );
		entity.setSomeInteger( 6 );
		entity.setSomeLong( 10L );
		entityManagerFactoryScope().inTransaction( entityManager -> entityManager.persist( entity ) );

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final SimpleEntity loaded = entityManager.find( SimpleEntity.class, 3 );
					assertThat( loaded, CoreMatchers.notNullValue() );
					assertThat( loaded.getSomeString(), CoreMatchers.is( "hello world" ) );
					assertThat( loaded.getSomeLong(), CoreMatchers.is( 10L ) );
				}
		);

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					entityManager.remove( entityManager.find( SimpleEntity.class, entity.getId() ) );
				}
		);

		entityManagerFactoryScope().inTransaction(
				entityManager -> {
					final SimpleEntity loaded = entityManager.find( SimpleEntity.class, entity.getId() );
					assertThat( loaded, CoreMatchers.nullValue() );
				}
		);
	}
}
