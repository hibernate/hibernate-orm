/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.dynamic;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;
import junit.framework.Assert;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
@TestForIssue(jiraKey = "HHH-8049")
public class AuditedDynamicComponentTest extends BaseEnversFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "mappings/dynamicComponents/mapAudited.hbm.xml" };
	}

	//@Test
	public void testAuditedDynamicComponentFailure() throws URISyntaxException {
		final Configuration config = new Configuration();
		final URL hbm = Thread.currentThread().getContextClassLoader().getResource(
				"mappings/dynamicComponents/mapAudited.hbm.xml"
		);
		config.addFile( new File( hbm.toURI() ) );

		final String auditStrategy = getAuditStrategy();
		if ( !StringTools.isEmpty( auditStrategy ) ) {
			config.setProperty( EnversSettings.AUDIT_STRATEGY, auditStrategy );
		}

		final ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() );
		try {
			config.buildSessionFactory( serviceRegistry );
			Assert.fail( "MappingException expected" );
		}
		catch ( MappingException e ) {
			Assert.assertEquals(
					"Audited dynamic-component properties are not supported. Consider applying @NotAudited annotation to "
							+ AuditedDynamicComponentEntity.class.getName() + "#customFields.",
					e.getMessage()
			);
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@Priority(10)
	public void initData() {
		Session session = openSession();

		SimpleEntity simpleEntity = new SimpleEntity( 1L, "Very simple entity" );

		// Revision 1
		session.getTransaction().begin();
		session.save( simpleEntity );
		session.getTransaction().commit();

		// Revision 2
		session.getTransaction().begin();
		AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
		entity.getCustomFields().put( "prop1", 13 );
		entity.getCustomFields().put( "prop2", 0.1f );
		entity.getCustomFields().put( "prop3", simpleEntity );
		entity.getCustomFields().put( "prop4", true );
		session.save( entity );
		session.getTransaction().commit();

		// revision 3
		session.getTransaction().begin();
		SimpleEntity simpleEntity2 = new SimpleEntity( 2L, "Not so simple entity" );
		session.save( simpleEntity2 );
		entity = (AuditedDynamicComponentEntity) session.get( AuditedDynamicComponentEntity.class, entity.getId() );
		entity.getCustomFields().put( "prop3", simpleEntity2 );
		session.update( entity );
		session.getTransaction().commit();

		// Revision 4
		session.getTransaction().begin();
		entity = (AuditedDynamicComponentEntity) session.get( AuditedDynamicComponentEntity.class, entity.getId() );
		entity.getCustomFields().put( "prop1", 2 );
		entity.getCustomFields().put( "prop4", false );
		session.update( entity );
		session.getTransaction().commit();

		// Revision 5
		session.getTransaction().begin();
		entity = (AuditedDynamicComponentEntity) session.load( AuditedDynamicComponentEntity.class, entity.getId() );
		entity.getCustomFields().remove( "prop2" );
		session.update( entity );
		session.getTransaction().commit();

		// Revision 6
		session.getTransaction().begin();
		entity = (AuditedDynamicComponentEntity) session.load( AuditedDynamicComponentEntity.class, entity.getId() );
		entity.getCustomFields().clear();
		session.update( entity );
		session.getTransaction().commit();

		// Revision 7
		session.getTransaction().begin();
		entity = (AuditedDynamicComponentEntity) session.load( AuditedDynamicComponentEntity.class, entity.getId() );
		session.delete( entity );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void testRevisionsCounts() {
		Assert.assertEquals(
				Arrays.asList( 2, 3, 4, 5, 6, 7 ),
				getAuditReader().getRevisions( AuditedDynamicComponentEntity.class, 1L )
		);
	}

	@Test
	public void testHistoryOfId1() {
		// Revision 2
		AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
		entity.getCustomFields().put( "prop1", 13 );
		entity.getCustomFields().put( "prop2", 0.1f );
		entity.getCustomFields().put( "prop3", new SimpleEntity( 1L, "Very simple entity" ) );
		entity.getCustomFields().put( "prop4", true );
		AuditedDynamicComponentEntity ver2 = getAuditReader().find(
				AuditedDynamicComponentEntity.class,
				entity.getId(),
				2
		);
		Assert.assertEquals( entity, ver2 );

		// Revision 3
		SimpleEntity simpleEntity2 = new SimpleEntity( 2L, "Not so simple entity" );
		entity.getCustomFields().put( "prop3", simpleEntity2 );
		AuditedDynamicComponentEntity ver3 = getAuditReader().find(
				AuditedDynamicComponentEntity.class,
				entity.getId(),
				3
		);
		Assert.assertEquals( entity, ver3 );

		// Revision 4
		entity.getCustomFields().put( "prop1", 2 );
		entity.getCustomFields().put( "prop4", false );
		AuditedDynamicComponentEntity ver4 = getAuditReader().find(
				AuditedDynamicComponentEntity.class,
				entity.getId(),
				4
		);
		Assert.assertEquals( entity, ver4 );

		// Revision 5
		entity.getCustomFields().put( "prop2", null );
		AuditedDynamicComponentEntity ver5 = getAuditReader().find(
				AuditedDynamicComponentEntity.class,
				entity.getId(),
				5
		);
		Assert.assertEquals( entity, ver5 );

		// Revision 5
		entity.getCustomFields().put( "prop1", null );
		entity.getCustomFields().put( "prop2", null );
		entity.getCustomFields().put( "prop3", null );
		entity.getCustomFields().put( "prop4", null );
		AuditedDynamicComponentEntity ver6 = getAuditReader().find(
				AuditedDynamicComponentEntity.class,
				entity.getId(),
				6
		);
		Assert.assertEquals( entity, ver6 );
	}


	@Test
	public void testOfQueryOnDynamicComponent() {
		//given (and result of initData()
		AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
		entity.getCustomFields().put( "prop1", 13 );
		entity.getCustomFields().put( "prop2", 0.1f );
		entity.getCustomFields().put( "prop3", new SimpleEntity( 1L, "Very simple entity" ) );
		entity.getCustomFields().put( "prop4", true );

		//when
		List resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 2 )
				.add( AuditEntity.property( "customFields_prop1" ).le( 20 ) )
				.getResultList();

		//then
		Assert.assertEquals( entity, resultList.get( 0 ) );

		//when
		resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 2 )
				.add( AuditEntity.property( "customFields_prop3" ).eq( new SimpleEntity( 1L, "Very simple entity" ) ) )
				.getResultList();


		//then
		entity = (AuditedDynamicComponentEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 4 )
				.getResultList().get( 0 );
		entity.getCustomFields().put( "prop2", null );

		resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 5 )
				.add( AuditEntity.property( "customFields_prop2" ).isNull() )
				.getResultList();

		//then
		Assert.assertEquals( entity, resultList.get( 0 ) );
	}

}
