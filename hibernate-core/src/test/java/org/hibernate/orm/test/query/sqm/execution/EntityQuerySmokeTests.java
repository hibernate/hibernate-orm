/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import java.time.Instant;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneJoinTable;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class EntityQuerySmokeTests extends SessionFactoryBasedFunctionalTest {
	@BeforeAll
	public void setUpTestData() {
		sessionFactoryScope().inTransaction(
				session -> {
					final EntityOfBasics entityOfBasics = new EntityOfBasics();
					entityOfBasics.setId( 1 );
					entityOfBasics.setGender( EntityOfBasics.Gender.MALE );
					entityOfBasics.setConvertedGender( EntityOfBasics.Gender.MALE );
					entityOfBasics.setOrdinalGender( EntityOfBasics.Gender.MALE );
					entityOfBasics.setTheInt( -1 );
					session.save( entityOfBasics );

					final SimpleEntity simpleEntity = new SimpleEntity();
					simpleEntity.setId( 1 );
					simpleEntity.setSomeInteger( -1 );
					simpleEntity.setSomeString( "the string" );
					simpleEntity.setSomeInstant( Instant.now() );
					session.save( simpleEntity );

					final EntityWithManyToOneJoinTable entityWithManyToOneJoinTable = new EntityWithManyToOneJoinTable(  );
					entityWithManyToOneJoinTable.setId( 1 );
					entityWithManyToOneJoinTable.setOther( simpleEntity );
					session.save( entityWithManyToOneJoinTable );
				}
		);

	}

	@AfterAll
	public void cleanUpTestData() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.createQuery( "delete EntityOfBasics" ).executeUpdate();
					session.createQuery( "delete EntityWithManyToOneJoinTable" ).executeUpdate();
					session.createQuery( "delete SimpleEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testRootEntitySelection() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e from EntityOfBasics e" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final EntityOfBasics entity = cast(
							value,
							EntityOfBasics.class
					);
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getGender(), is( EntityOfBasics.Gender.MALE ) );
					assertThat( entity.getTheInt(), is( -1 ) );
					assertThat( entity.getTheInteger(), nullValue() );
					assertThat( entity.getOrdinalGender(), is( EntityOfBasics.Gender.MALE ) );
					assertThat( entity.getConvertedGender(), is( EntityOfBasics.Gender.MALE ) );
				}
		);
	}

	@Test
	public void testRootEntityAttributeSelection() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e.id from EntityOfBasics e" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					Integer id = cast(
							value,
							Integer.class
					);
					assertThat( id, is( 1 ) );
				}
		);
	}

	@Test
	public void testRootEntityAttributeReference() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e from EntityOfBasics e where id = 1" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final EntityOfBasics entity = cast(
							value,
							EntityOfBasics.class
					);
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getGender(), is( EntityOfBasics.Gender.MALE ) );
					assertThat( entity.getTheInt(), is( -1 ) );
					assertThat( entity.getTheInteger(), nullValue() );
					assertThat( entity.getOrdinalGender(), is( EntityOfBasics.Gender.MALE ) );
					assertThat( entity.getConvertedGender(), is( EntityOfBasics.Gender.MALE ) );
				}
		);
	}

	@Test
	public void testRootEntityManyToOneSelection() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e.other from EntityWithManyToOneJoinTable e" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final SimpleEntity entity = cast( value, SimpleEntity.class );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getSomeString(), is( "the string" ) );
				}
		);
	}

	@Test
	public void testRootEntityManyToOneAttributeReference() {
		sessionFactoryScope().inSession(
				session -> {
					final List result = session.createQuery( "select e.other from EntityWithManyToOneJoinTable e" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, notNullValue() );
					final SimpleEntity entity = cast( value, SimpleEntity.class );
					assertThat( entity.getId(), is( 1 ) );
					assertThat( entity.getSomeString(), is( "the string" ) );
				}
		);
	}

	@Test
	public void testJoinedSubclassRoot() {
		sessionFactoryScope().inSession(
				session -> session.createQuery( "select p from Payment p" ).list()
		);
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
		StandardDomainModel.RETAIL.getDescriptor().applyDomainModel( metadataSources );
	}
}
