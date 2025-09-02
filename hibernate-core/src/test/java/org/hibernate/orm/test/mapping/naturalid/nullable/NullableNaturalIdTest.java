/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.nullable;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { A.class, B.class, C.class, D.class },
		xmlMappings = "/org/hibernate/orm/test/mapping/naturalid/nullable/User.hbm.xml"
)
@SessionFactory
public class NullableNaturalIdTest {

	@Test
	@JiraKey( value = "HHH-10360")
	public void testNaturalIdNullability(SessionFactoryScope scope) {
		// A, B, C, and D are mapped using annotations;
		// none are mapped to be non-nullable, so all are nullable by annotations-specific default,
		// except primitives

		// NOTE: for each entity we check against both the runtime mapping API and the legacy persister API

		{
			final EntityMappingType entityMappingType = scope.getSessionFactory().getRuntimeMetamodels().getEntityMappingType( A.class );
			final NaturalIdMapping naturalIdMapping = entityMappingType.getNaturalIdMapping();
			final EntityPersister persister = entityMappingType.getEntityPersister();

			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "assC" )] );
			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "myname" )] );
			assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 2 ) );

			final SingularAttributeMapping firstAttribute = naturalIdMapping.getNaturalIdAttributes().get(0);
			assertThat( firstAttribute.getAttributeName(), is( "assC" ) );
			assertThat( firstAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "assC" ) ) );

			final SingularAttributeMapping secondAttribute = naturalIdMapping.getNaturalIdAttributes().get(1);
			assertThat( secondAttribute.getAttributeName(), is( "myname" ) );
			assertThat( secondAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "myname" ) ) );
		}

		{
			final EntityMappingType entityMappingType = scope.getSessionFactory().getRuntimeMetamodels().getEntityMappingType( B.class );
			final NaturalIdMapping naturalIdMapping = entityMappingType.getNaturalIdMapping();
			final EntityPersister persister = entityMappingType.getEntityPersister();

			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "assA" )] );
			assertFalse( persister.getPropertyNullability()[persister.getPropertyIndex( "naturalid" )] );
			assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 2 ) );

			final SingularAttributeMapping firstAttribute = naturalIdMapping.getNaturalIdAttributes().get(0);
			assertThat( firstAttribute.getAttributeName(), is( "assA" ) );
			assertThat( firstAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "assA" ) ) );
			assertTrue( firstAttribute.getAttributeMetadata().isNullable() );

			final SingularAttributeMapping secondAttribute = naturalIdMapping.getNaturalIdAttributes().get(1);
			assertThat( secondAttribute.getAttributeName(), is( "naturalid" ) );
			assertThat( secondAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "naturalid" ) ) );
			assertFalse( secondAttribute.getAttributeMetadata().isNullable() );
		}

		{
			final EntityMappingType entityMappingType = scope.getSessionFactory().getRuntimeMetamodels().getEntityMappingType( C.class );
			final NaturalIdMapping naturalIdMapping = entityMappingType.getNaturalIdMapping();
			final EntityPersister persister = entityMappingType.getEntityPersister();

			assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 1 ) );
			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "name" )] );

			final SingularAttributeMapping attribute = naturalIdMapping.getNaturalIdAttributes().get(0);
			assertThat( attribute.getStateArrayPosition(), is( persister.getPropertyIndex( "name" ) ) );
			assertTrue( attribute.getAttributeMetadata().isNullable() );
		}

		{
			final EntityMappingType entityMappingType = scope.getSessionFactory().getRuntimeMetamodels().getEntityMappingType( D.class );
			final NaturalIdMapping naturalIdMapping = entityMappingType.getNaturalIdMapping();
			final EntityPersister persister = entityMappingType.getEntityPersister();

			assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 2 ) );
			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "associatedC" )] );
			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "name" )] );

			final SingularAttributeMapping firstAttribute = naturalIdMapping.getNaturalIdAttributes().get(0);
			assertThat( firstAttribute.getAttributeName(), is( "associatedC" ) );
			assertThat( firstAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "associatedC" ) ) );
			assertTrue( firstAttribute.getAttributeMetadata().isNullable() );

			final SingularAttributeMapping secondAttribute = naturalIdMapping.getNaturalIdAttributes().get(1);
			assertThat( secondAttribute.getAttributeName(), is( "name" ) );
			assertThat( secondAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "name" ) ) );
			assertTrue( secondAttribute.getAttributeMetadata().isNullable() );
		}

		{
			// User is mapped using hbm.xml; properties are explicitly mapped to be nullable

			final EntityMappingType entityMappingType = scope.getSessionFactory().getRuntimeMetamodels().getEntityMappingType( User.class );
			final NaturalIdMapping naturalIdMapping = entityMappingType.getNaturalIdMapping();
			final EntityPersister persister = entityMappingType.getEntityPersister();

			assertThat( naturalIdMapping.getNaturalIdAttributes().size(), is( 3 ) );
			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "intVal" )] );
			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "name" )] );
			assertTrue( persister.getPropertyNullability()[persister.getPropertyIndex( "org" )] );

			final SingularAttributeMapping firstAttribute = naturalIdMapping.getNaturalIdAttributes().get(0);
			assertThat( firstAttribute.getAttributeName(), is( "intVal" ) );
			assertThat( firstAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "intVal" ) ) );
			assertTrue( firstAttribute.getAttributeMetadata().isNullable() );

			final SingularAttributeMapping secondAttribute = naturalIdMapping.getNaturalIdAttributes().get(1);
			assertThat( secondAttribute.getAttributeName(), is( "name" ) );
			assertThat( secondAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "name" ) ) );
			assertTrue( secondAttribute.getAttributeMetadata().isNullable() );

			final SingularAttributeMapping thirdAttribute = naturalIdMapping.getNaturalIdAttributes().get(2);
			assertThat( thirdAttribute.getAttributeName(), is( "org" ) );
			assertThat( thirdAttribute.getStateArrayPosition(), is( persister.getPropertyIndex( "org" ) ) );
			assertTrue( thirdAttribute.getAttributeMetadata().isNullable() );
		}
	}

	@Test
	public void testNaturalIdNullValueOnPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// this should be fine because the natural-id is nullable
					final C c = new C( 1, null );
					session.persist( c );
				}
		);

		scope.inTransaction(
				(session) -> {
					final C c = session.get(C.class, 1);
					assertThat( c, notNullValue() );
					assertThat( c.name, nullValue() );
				}
		);
	}

	@Test
	public void testUniqueAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final A a = new A( 1, null, null );
					final B b = new B( 1, null, 100 );
					session.persist( a );
					session.persist( b );
				}
		);

		scope.inTransaction(
				(session) -> {
					final A a = session.get( A.class, 1 );
					assertThat( a, notNullValue() );
					assertThat( a.assC, nullValue() );
					assertThat( a.myname, nullValue() );

					final B b = session.get( B.class, 1 );
					assertThat( b, notNullValue() );
					assertThat( b.assA, nullValue() );
					assertThat( b.naturalid, notNullValue() );
					b.assA = a;
					a.assB.add( b );
				}
		);

		scope.inTransaction(
				(session) -> {
					final B b = session.get( B.class, 1 );
					assertThat( b, notNullValue() );
					assertThat( b.assA, notNullValue() );
				}
		);
	}

	@Test
	public void testNaturalIdQuerySupportingNullValues(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final C c = new C( 1, null );
					final D d1 = new D( 1, "Titi", null );
					final D d2 = new D( 2, null, c );
					session.persist( c );
					session.persist( d1 );
					session.persist( d2 );
				}
		);

		scope.inTransaction(
				(session) -> {
					final D d1 = session.byNaturalId( D.class )
							.using( "name", "Titi" )
							.using( "associatedC", null )
							.load();
					assertThat( d1, notNullValue() );
					assertThat( d1.name, notNullValue() );
					assertThat( d1.associatedC, nullValue() );

					final C cRef = session.getReference( C.class, 1 );
					final D d2 = session.byNaturalId( D.class )
							.using( "name", null )
							.using( "associatedC", cRef )
							.load();
					assertThat( d2, notNullValue() );
					assertThat( d2.name, nullValue() );
					assertThat( d2.associatedC, notNullValue() );
				}
		);

		scope.inTransaction(
				(session) -> {
					final D d1 = session.bySimpleNaturalId( D.class ).load( new Object[] { null, "Titi" } );
					assertThat( d1, notNullValue() );
					final C cRef = session.getReference( C.class, 1 );
					final D d2 = session.bySimpleNaturalId( D.class ).load( new Object[] { cRef, null } );
					assertThat( d2, notNullValue() );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
