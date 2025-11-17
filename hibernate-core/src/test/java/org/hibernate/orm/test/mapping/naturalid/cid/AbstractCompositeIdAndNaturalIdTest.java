/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.cid;

import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCompositeIdAndNaturalIdTest {
	@Test
	@JiraKey( value = "HHH-10360")
	public void testNaturalIdNullability(SessionFactoryScope scope) {
		final EntityMappingType accountMapping = scope.getSessionFactory().getRuntimeMetamodels().getEntityMappingType( Account.class );
		final SingularAttributeMapping shortCodeMapping = ((SimpleNaturalIdMapping) accountMapping.getNaturalIdMapping()).getAttribute();
		final AttributeMetadata shortCodeMetadata = shortCodeMapping.getAttributeMetadata();
		assertThat( shortCodeMetadata.isNullable(), is( false ) );

		final EntityPersister rootEntityPersister = accountMapping.getRootEntityDescriptor().getEntityPersister();
		final int shortCodeLegacyPropertyIndex = rootEntityPersister.getPropertyIndex( "shortCode" );
		assertThat( shortCodeLegacyPropertyIndex, is ( 0 ) );
		assertThat( rootEntityPersister.getPropertyNullability()[ shortCodeLegacyPropertyIndex ], is( false ) );
	}

	public static final String NATURAL_ID_VALUE = "testAcct";

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					// prepare some test data...
					Account account = new Account( new AccountId( 1 ), NATURAL_ID_VALUE );
					session.persist( account );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testNaturalIdCriteria(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Account account = session.bySimpleNaturalId( Account.class ).load( NATURAL_ID_VALUE );
					assertThat( account, notNullValue() );
				}
		);
	}

	@Test
	public void testNaturalIdApi(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Account account = session.bySimpleNaturalId( Account.class ).load( NATURAL_ID_VALUE );
					assertThat( account, notNullValue() );
				}
		);
		scope.inTransaction(
				(session) -> {
					final Account account = session.byNaturalId( Account.class ).using( "shortCode", NATURAL_ID_VALUE ).load();
					assertThat( account, notNullValue() );
				}
		);
	}
}
