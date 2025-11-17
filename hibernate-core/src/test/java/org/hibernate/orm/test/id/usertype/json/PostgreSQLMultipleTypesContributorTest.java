/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.json;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.orm.test.id.usertype.inet.Inet;
import org.hibernate.orm.test.id.usertype.inet.InetJavaType;
import org.hibernate.orm.test.id.usertype.inet.InetJdbcType;
import org.hibernate.orm.test.id.usertype.inet.InetType;
import org.hibernate.query.NativeQuery;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
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
		impl = PostgreSQLMultipleTypesContributorTest.TypeContributorImpl.class
) )
@DomainModel(annotatedClasses = PostgreSQLMultipleTypesContributorTest.Event.class)
@SessionFactory
public class PostgreSQLMultipleTypesContributorTest {

	@BeforeEach
	public void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			var event = new Event();
			event.setId( 1L );
			event.setIp( "192.168.0.123/24" );
			event.setProperties( new Json( "{\"key\": \"temp\", \"value\": \"9C\"}" ) );

			session.persist( event );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testMultipleTypeContributions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//noinspection removal
			var inets = entityManager.createNativeQuery(
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
	public void testMultipleTypeContributionsExplicitBinding(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//noinspection unchecked,deprecation
			List<Inet> inets = entityManager.createNativeQuery(
				"select e.ip " +
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

	@Entity(name = "Event")
	@Table(name = "event")
	public static class Event {

		@Id
		private Long id;

		@Column(name = "ip")
		@JdbcTypeCode(SqlTypes.INET)
		@JavaType(InetJavaType.class)
		private Inet ip;

		@Column(name = "properties")
		@JdbcTypeCode(SqlTypes.JSON)
		@JavaType(JsonJavaType.class)
		private Json properties;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Inet getIp() {
			return ip;
		}

		public void setIp(String address) {
			this.ip = new Inet( address );
		}

		public Json getProperties() {
			return properties;
		}

		public void setProperties(Json properties) {
			this.properties = properties;
		}
	}

	public static class TypeContributorImpl implements TypeContributor {
		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJavaType( InetJavaType.INSTANCE );
			typeContributions.contributeJdbcType( InetJdbcType.INSTANCE );

			var typeConfiguration = typeContributions.getTypeConfiguration();
			typeConfiguration.getBasicTypeRegistry().register( InetType.INSTANCE );
			typeConfiguration.getBasicTypeRegistry().register( JsonType.INSTANCE );
		}
	}
}
