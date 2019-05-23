/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.dynamic;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.internal.tools.StringTools;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.dynamic.AuditedDynamicComponentEntity;
import org.hibernate.envers.test.support.domains.components.dynamic.SimpleEntity;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Lukasz Zuchowski (author at zuchos dot com)
 */
@TestForIssue(jiraKey = "HHH-8049")
public class AuditedDynamicComponentTest extends EnversSessionFactoryBasedFunctionalTest {

	@Override
	protected String[] getMappings() {
		return new String[] { "dynamic-components/MapAudited.hbm.xml" };
	}

	public void testAuditedDynamicComponentFailure() throws URISyntaxException {
		final Configuration config = new Configuration();
		final URL hbm = Thread.currentThread().getContextClassLoader().getResource(
				"dynamic-components/MapAudited.hbm.xml"
		);
		config.addFile( new File( hbm.toURI() ) );

		final String auditStrategy = getAuditService().getOptions().getAuditStrategy().getClass().getName();
		if ( !StringTools.isEmpty( auditStrategy ) ) {
			config.setProperty( EnversSettings.AUDIT_STRATEGY, auditStrategy );
		}

		final ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() );
		try {
			config.buildSessionFactory( serviceRegistry );
			fail( "MappingException expected" );
		}
		catch ( MappingException e ) {
			final StringBuilder message = new StringBuilder();
			message.append( "Audited dynamic-component properties are not supported. " );
			message.append( "Consider applying @NotAudited annotation to " );
			message.append( AuditedDynamicComponentEntity.class.getName() ).append( "#customFields." );
			assertThat( e.getMessage(), equalTo( message.toString() ) );
		}
		finally {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		final SimpleEntity simpleEntity = new SimpleEntity( 1L, "Very simple entity" );
		final Long dynamicId = 1L;

		inTransactions(
				// Revision 1
				session -> {
					session.save( simpleEntity );
				},

				// Revision 2
				session -> {
					AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( dynamicId, "static field value" );
					entity.getCustomFields().put( "prop1", 13 );
					entity.getCustomFields().put( "prop2", 0.1f );
					entity.getCustomFields().put( "prop3", simpleEntity );
					entity.getCustomFields().put( "prop4", true );
					session.save( entity );
				},

				// Revision 3
				session -> {
					SimpleEntity simpleEntity2 = new SimpleEntity( 2L, "Not so simple entity" );
					session.save( simpleEntity2 );
					AuditedDynamicComponentEntity entity = session.get( AuditedDynamicComponentEntity.class, dynamicId );
					entity.getCustomFields().put( "prop3", simpleEntity2 );
					session.update( entity );
				},

				// Revision 4
				session -> {
					AuditedDynamicComponentEntity entity = session.get( AuditedDynamicComponentEntity.class, dynamicId );
					entity.getCustomFields().put( "prop1", 2 );
					entity.getCustomFields().put( "prop4", false );
					session.update( entity );
				},

				// Revision 5
				session -> {
					AuditedDynamicComponentEntity entity = session.load( AuditedDynamicComponentEntity.class, dynamicId );
					entity.getCustomFields().remove( "prop2" );
					session.update( entity );
				},

				// Revision 6
				session -> {
					AuditedDynamicComponentEntity entity = session.load( AuditedDynamicComponentEntity.class, dynamicId );
					entity.getCustomFields().clear();
					session.update( entity );
				},

				// Revision 7
				session -> {
					AuditedDynamicComponentEntity entity = session.load( AuditedDynamicComponentEntity.class, dynamicId );
					session.delete( entity );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( AuditedDynamicComponentEntity.class, 1L ), contains( 2, 3, 4, 5, 6, 7 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		// Revision 2
		AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
		entity.getCustomFields().put( "prop1", 13 );
		entity.getCustomFields().put( "prop2", 0.1f );
		entity.getCustomFields().put( "prop3", new SimpleEntity( 1L, "Very simple entity" ) );
		entity.getCustomFields().put( "prop4", true );
		assertThat( getAuditReader().find( AuditedDynamicComponentEntity.class, entity.getId(), 2 ), equalTo( entity ) );

		// Revision 3
		SimpleEntity simpleEntity2 = new SimpleEntity( 2L, "Not so simple entity" );
		entity.getCustomFields().put( "prop3", simpleEntity2 );
		assertThat( getAuditReader().find( AuditedDynamicComponentEntity.class, entity.getId(), 3 ), equalTo( entity ) );

		// Revision 4
		entity.getCustomFields().put( "prop1", 2 );
		entity.getCustomFields().put( "prop4", false );
		assertThat( getAuditReader().find( AuditedDynamicComponentEntity.class, entity.getId(), 4 ), equalTo( entity ) );

		// Revision 5
		entity.getCustomFields().put( "prop2", null );
		assertThat( getAuditReader().find( AuditedDynamicComponentEntity.class, entity.getId(), 5 ), equalTo( entity ) );

		// Revision 5
		entity.getCustomFields().put( "prop1", null );
		entity.getCustomFields().put( "prop2", null );
		entity.getCustomFields().put( "prop3", null );
		entity.getCustomFields().put( "prop4", null );
		assertThat( getAuditReader().find( AuditedDynamicComponentEntity.class, entity.getId(), 6 ), equalTo( entity ) );
	}


	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testOfQueryOnDynamicComponent() {
		AuditedDynamicComponentEntity entity = new AuditedDynamicComponentEntity( 1L, "static field value" );
		entity.getCustomFields().put( "prop1", 13 );
		entity.getCustomFields().put( "prop2", 0.1f );
		entity.getCustomFields().put( "prop3", new SimpleEntity( 1L, "Very simple entity" ) );
		entity.getCustomFields().put( "prop4", true );

		List<AuditedDynamicComponentEntity> resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 2 )
				.add( AuditEntity.property( "customFields_prop1" ).le( 20 ) )
				.getResultList();

		assertThat( resultList.get( 0 ), equalTo( entity ) );

		getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 2 )
				.add( AuditEntity.property( "customFields_prop3" ).eq( new SimpleEntity( 1L, "Very simple entity" ) ) )
				.getResultList();

		entity = (AuditedDynamicComponentEntity) getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 4 )
				.getResultList().get( 0 );
		entity.getCustomFields().put( "prop2", null );

		resultList = getAuditReader().createQuery()
				.forEntitiesAtRevision( AuditedDynamicComponentEntity.class, 5 )
				.add( AuditEntity.property( "customFields_prop2" ).isNull() )
				.getResultList();

		//then
		assertThat( resultList.get( 0 ), equalTo( entity ) );
	}

}
