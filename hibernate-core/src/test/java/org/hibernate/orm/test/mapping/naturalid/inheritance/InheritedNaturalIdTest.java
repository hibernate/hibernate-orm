/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry( settings = @Setting( name= GENERATE_STATISTICS, value = "true" ) )
@DomainModel( annotatedClasses = { Principal.class, User.class, System.class } )
@SessionFactory
public class InheritedNaturalIdTest {
	@Test
	@JiraKey( value = "HHH-10360")
	public void verifyMappingModel(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		final EntityMappingType userMapping = sessionFactory.getRuntimeMetamodels().getEntityMappingType( User.class );

		final SingularAttributeMapping uidMapping = ((SimpleNaturalIdMapping) userMapping.getNaturalIdMapping()).getAttribute();
		assertThat( uidMapping.getAttributeName(), is ("uid" ) );
		final AttributeMetadata uidMetadata = uidMapping.getAttributeMetadata();
		assertThat( uidMetadata.isNullable(), is( true ) );

		final EntityPersister rootEntityPersister = userMapping.getEntityPersister();
		final int uidLegacyPropertyIndex = rootEntityPersister.getEntityMetamodel().getPropertyIndex( "uid" );
		assertThat( uidLegacyPropertyIndex, is ( 0 ) );
		assertThat( rootEntityPersister.getPropertyNullability()[ uidLegacyPropertyIndex ], is( true ) );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.persist( new User( ORIGINAL ) )
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	public static final String ORIGINAL = "steve";

	@Test
	public void testNaturalIdApi(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Principal principalBySimple = session.bySimpleNaturalId( Principal.class ).load( ORIGINAL );
					assertThat( principalBySimple, notNullValue() );

					final Principal principal = session.byNaturalId( Principal.class ).using( "uid", ORIGINAL ).load();
					assertThat( principal, notNullValue() );

					final Principal userBySimple = session.bySimpleNaturalId( User.class ).load( ORIGINAL );
					assertThat( userBySimple, notNullValue() );

					final Principal user = session.byNaturalId( User.class ).using( "uid", ORIGINAL ).load();
					assertThat( user, notNullValue() );

					assertThat( principalBySimple, is( principal ) );
					assertThat( principalBySimple, is( userBySimple ) );
					assertThat( principalBySimple, is( user ) );
				}
		);
	}


	@Test
	public void testSubclassDeleteNaturalId(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final Principal p = session.bySimpleNaturalId( Principal.class ).load( ORIGINAL );
					assertNotNull( p );

					session.remove( p );
					session.flush();
				}
		);

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final Principal p = session.bySimpleNaturalId( Principal.class ).load( ORIGINAL );
					assertThat( p, nullValue() );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testWrongClassLoadingById(SessionFactoryScope scope) {
		// without caching enabled (even without the row being cached if it is enabled),
		// this simply returns null rather than throwing WrongClassException
		// todo (6.0) : do we want to make this more consistent?

		scope.inTransaction(
				(session) -> {
					final System loaded = session.byId( System.class ).load( 1L );
					assertThat( loaded, nullValue() );
				}

		);
	}

	@Test
	public void testWrongClassLoadingByNaturalId(SessionFactoryScope scope) {
		// without caching enabled (and even without the row being cached if it is enabled),
		// this simply returns null rather than throwing WrongClassException
		// todo (6.0) : do we want to make this more consistent?

		scope.inTransaction(
				(session) -> {
					final System loaded = session.bySimpleNaturalId( System.class ).load( ORIGINAL );
					assertThat( loaded, nullValue() );
				}
		);
	}
}
