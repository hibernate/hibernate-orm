/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.dynamic;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.envers.EnversEntityManagerFactoryScope.getAuditStrategy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
@JiraKey("HHH-8049")
@EnversTest
@Jpa(
		xmlMappings = "mappings/dynamicComponents/mapAudited.hbm.xml",
		annotatedClasses = {AuditedDynamicComponentEntity.class, SimpleEntity.class}
)
public class AuditedDynamicComponentTest {

	//@Test
	public void testAuditedDynamicComponentFailure(EntityManagerFactoryScope scope) throws URISyntaxException {
		final Configuration config = new Configuration();
		final URL hbm = Thread.currentThread().getContextClassLoader().getResource(
				"mappings/dynamicComponents/mapAudited.hbm.xml"
		);
		config.addFile( new File( hbm.toURI() ) );

		scope.inEntityManager( em -> {
			final var auditStrategyClass = getAuditStrategy( em ).getClass();
			if ( auditStrategyClass != DefaultAuditStrategy.class ) {
				config.setProperty( EnversSettings.AUDIT_STRATEGY, auditStrategyClass.getName() );
			}
		} );

		final ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() );
		try {
			config.buildSessionFactory( serviceRegistry ).close();
			fail( "MappingException expected" );
		}
		catch ( MappingException e ) {
			assertEquals(
					"Audited dynamic-component properties are not supported. Consider applying @NotAudited annotation to "
							+ AuditedDynamicComponentEntity.class.getName() + "#customFields.",
					e.getMessage()
			);
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			SimpleEntity simpleEntity = new SimpleEntity( 1L, "Very simple entity" );
			em.persist( simpleEntity );
		} );

		// Revision 2
		scope.inTransaction( em -> {
			SimpleEntity simpleEntity = em.find( SimpleEntity.class, 1L );
			AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
			entity.getCustomFields().put( "prop1", 13 );
			entity.getCustomFields().put( "prop2", 0.1f );
			entity.getCustomFields().put( "prop3", simpleEntity );
			entity.getCustomFields().put( "prop4", true );
			em.persist( entity );
		} );

		// revision 3
		scope.inTransaction( em -> {
			SimpleEntity simpleEntity2 = new SimpleEntity( 2L, "Not so simple entity" );
			em.persist( simpleEntity2 );
			AuditedDynamicComponentEntity entity = em.find( AuditedDynamicComponentEntity.class, 1L );
			entity.getCustomFields().put( "prop3", simpleEntity2 );
			em.merge( entity );
		} );

		// Revision 4
		scope.inTransaction( em -> {
			AuditedDynamicComponentEntity entity = em.find( AuditedDynamicComponentEntity.class, 1L );
			entity.getCustomFields().put( "prop1", 2 );
			entity.getCustomFields().put( "prop4", false );
			em.merge( entity );
		} );

		// Revision 5
		scope.inTransaction( em -> {
			AuditedDynamicComponentEntity entity = em.getReference( AuditedDynamicComponentEntity.class, 1L );
			entity.getCustomFields().remove( "prop2" );
			em.merge( entity );
		} );

		// Revision 6
		scope.inTransaction( em -> {
			AuditedDynamicComponentEntity entity = em.getReference( AuditedDynamicComponentEntity.class, 1L );
			entity.getCustomFields().clear();
			em.merge( entity );
		} );

		// Revision 7
		scope.inTransaction( em -> {
			AuditedDynamicComponentEntity entity = em.getReference( AuditedDynamicComponentEntity.class, 1L );
			em.remove( entity );
		} );
	}

	@Test
	public void testRevisionsCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals(
					Arrays.asList( 2, 3, 4, 5, 6, 7 ),
					AuditReaderFactory.get( em ).getRevisions( AuditedDynamicComponentEntity.class, 1L )
			);
		} );
	}

	@Test
	public void testHistoryOfId1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			// Revision 2
			AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
			entity.getCustomFields().put( "prop1", 13 );
			entity.getCustomFields().put( "prop2", 0.1f );
			entity.getCustomFields().put( "prop3", new SimpleEntity( 1L, "Very simple entity" ) );
			entity.getCustomFields().put( "prop4", true );
			AuditedDynamicComponentEntity ver2 = auditReader.find(
					AuditedDynamicComponentEntity.class,
					entity.getId(),
					2
			);
			assertEquals( entity, ver2 );

			// Revision 3
			SimpleEntity simpleEntity2 = new SimpleEntity( 2L, "Not so simple entity" );
			entity.getCustomFields().put( "prop3", simpleEntity2 );
			AuditedDynamicComponentEntity ver3 = auditReader.find(
					AuditedDynamicComponentEntity.class,
					entity.getId(),
					3
			);
			assertEquals( entity, ver3 );

			// Revision 4
			entity.getCustomFields().put( "prop1", 2 );
			entity.getCustomFields().put( "prop4", false );
			AuditedDynamicComponentEntity ver4 = auditReader.find(
					AuditedDynamicComponentEntity.class,
					entity.getId(),
					4
			);
			assertEquals( entity, ver4 );

			// Revision 5
			entity.getCustomFields().put( "prop2", null );
			AuditedDynamicComponentEntity ver5 = auditReader.find(
					AuditedDynamicComponentEntity.class,
					entity.getId(),
					5
			);
			assertEquals( entity, ver5 );

			// Revision 6
			entity.getCustomFields().put( "prop1", null );
			entity.getCustomFields().put( "prop2", null );
			entity.getCustomFields().put( "prop3", null );
			entity.getCustomFields().put( "prop4", null );
			AuditedDynamicComponentEntity ver6 = auditReader.find(
					AuditedDynamicComponentEntity.class,
					entity.getId(),
					6
			);
			assertEquals( entity, ver6 );
		} );
	}


	@Test
	public void testOfQueryOnDynamicComponent(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var auditReader = AuditReaderFactory.get( em );

			//given (and result of initData()
			AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
			entity.getCustomFields().put( "prop1", 13 );
			entity.getCustomFields().put( "prop2", 0.1f );
			entity.getCustomFields().put( "prop3", new SimpleEntity( 1L, "Very simple entity" ) );
			entity.getCustomFields().put( "prop4", true );

			//when
			List resultList = auditReader.createQuery()
					.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 2 )
					.add( AuditEntity.property( "customFields_prop1" ).le( 20 ) )
					.getResultList();

			//then
			assertEquals( entity, resultList.get( 0 ) );

			//when
			resultList = auditReader.createQuery()
					.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 2 )
					.add( AuditEntity.property( "customFields_prop3" ).eq( new SimpleEntity( 1L, "Very simple entity" ) ) )
					.getResultList();


			//then
			AuditedDynamicComponentEntity entity2 = (AuditedDynamicComponentEntity) auditReader.createQuery()
					.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 4 )
					.getResultList().get( 0 );
			entity2.getCustomFields().put( "prop2", null );

			resultList = auditReader.createQuery()
					.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 5 )
					.add( AuditEntity.property( "customFields_prop2" ).isNull() )
					.getResultList();

			//then
			assertEquals( entity2, resultList.get( 0 ) );
		} );
	}

}
