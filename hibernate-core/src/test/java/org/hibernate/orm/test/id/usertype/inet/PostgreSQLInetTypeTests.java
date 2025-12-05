/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.inet;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.query.NativeQuery;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialectFeature(feature = DialectFeatureChecks.IsPgJdbc.class)
@BootstrapServiceRegistry( javaServices = @BootstrapServiceRegistry.JavaService(
		role = TypeContributor.class,
		impl = PostgreSQLInetTypeTests.TypeContributorImpl.class
) )
@DomainModel(annotatedClasses = Event.class)
@SessionFactory
public class PostgreSQLInetTypeTests {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var event = new Event();
			event.setId( 1L );
			event.setIp( "192.168.0.123/24" );

			session.persist( event );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testJPQL(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			var inets = entityManager.createQuery(
				"select e.ip " +
				"from Event e " +
				"where e.id = :id", Inet.class )
			.setParameter( "id", 1L )
			.getResultList();

			Assertions.assertEquals( 1, inets.size() );
			Assertions.assertEquals( "192.168.0.123/24", inets.get( 0 ).getAddress() );
		} );
	}

	@Test
	public void testNativeSQLAddScalar(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			List<Inet> inets = entityManager.createNativeQuery(
				"select e.ip as ip " +
				"from Event e " +
				"where e.id = :id" )
			.setParameter( "id", 1L )
			.unwrap( NativeQuery.class )
			.addScalar( "ip", InetType.INSTANCE )
			.getResultList();

			Assertions.assertEquals( 1, inets.size() );
			Assertions.assertEquals( "192.168.0.123/24", inets.get( 0 ).getAddress() );
		} );
	}


	public static class TypeContributorImpl implements TypeContributor {
		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJavaType( InetJavaType.INSTANCE );
			typeContributions.contributeJdbcType( InetJdbcType.INSTANCE );

			var typeConfiguration = typeContributions.getTypeConfiguration();
			typeConfiguration.getBasicTypeRegistry().register( InetType.INSTANCE );
		}
	}
}
