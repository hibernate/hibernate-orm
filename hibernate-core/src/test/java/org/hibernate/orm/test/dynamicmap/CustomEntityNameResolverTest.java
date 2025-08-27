/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dynamicmap;

import org.hibernate.EntityNameResolver;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryExtension;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeParameterResolver;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-18486" )
@ServiceRegistry(settings = @Setting(name = SchemaToolingSettings.HBM2DDL_AUTO, value = "create-drop") )
@DomainModel(xmlMappings = "org/hibernate/orm/test/dynamicmap/artist.xml")
@ExtendWith( SessionFactoryExtension.class )
@ExtendWith( SessionFactoryScopeParameterResolver.class )
public class CustomEntityNameResolverTest implements SessionFactoryProducer {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		// Persist the dynamic map entity
		factoryScope.inTransaction( (session) -> {
			final Map<String, Object> artistEntity = new HashMap<>();
			artistEntity.put( "id", 1 );
			artistEntity.put( "name", "John Doe" );
			session.persist( artistEntity );
		} );

		factoryScope.inTransaction( (session) -> {
			//noinspection unchecked
			final Map<String, Object> loaded = (Map<String, Object>) session.byId( "artist" ).load( 1 );
			assertThat( loaded ).isNotNull();
			assertThat( loaded.get( "$type$" ) ).isEqualTo( "artist" );
		} );
	}


	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		return (SessionFactoryImplementor) model.getSessionFactoryBuilder()
				.addEntityNameResolver( new HibernateEntityNameResolver() )
				.build();
	}

	static class HibernateEntityNameResolver implements EntityNameResolver {
		@Override
		public String resolveEntityName(Object entity) {
			if ( entity instanceof Map ) {
				return "artist";
			}
			return null;
		}
	}
}
