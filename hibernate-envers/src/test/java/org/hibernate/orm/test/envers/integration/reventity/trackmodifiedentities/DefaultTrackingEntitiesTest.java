/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.trackmodifiedentities;

import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.tools.Pair;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.orm.test.envers.entities.StrIntTestEntity;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests proper behavior of tracking modified entity names when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@EnversTest
@DomainModel(annotatedClasses = {StrTestEntity.class, StrIntTestEntity.class})
@ServiceRegistry(settings = @Setting(name = EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, value = "true"))
@SessionFactory
public class DefaultTrackingEntitiesTest {
	private Integer steId = null;
	private Integer siteId = null;

	@BeforeClassTemplate
	public void initData(SessionFactoryScope scope) {
		scope.inSession( em -> {
			// Revision 1 - Adding two entities
			em.getTransaction().begin();
			StrTestEntity ste = new StrTestEntity( "x" );
			StrIntTestEntity site = new StrIntTestEntity( "y", 1 );
			em.persist( ste );
			em.persist( site );
			steId = ste.getId();
			siteId = site.getId();
			em.getTransaction().commit();

			// Revision 2 - Modifying one entity
			em.getTransaction().begin();
			site = em.find( StrIntTestEntity.class, siteId );
			site.setNumber( 2 );
			em.getTransaction().commit();

			// Revision 3 - Deleting both entities
			em.getTransaction().begin();
			ste = em.find( StrTestEntity.class, steId );
			site = em.find( StrIntTestEntity.class, siteId );
			em.remove( ste );
			em.remove( site );
			em.getTransaction().commit();
		} );
	}

	@Test
	public void testRevEntityTableCreation(DomainModelScope scope) {
		for ( Table table : scope.getDomainModel().collectTableMappings() ) {
			if ( "REVCHANGES".equals( table.getName() ) ) {
				assertEquals( 2, table.getColumnSpan() );
				assertNotNull( table.getColumn( new Column( "REV" ) ) );
				assertNotNull( table.getColumn( new Column( "ENTITYNAME" ) ) );
				return;
			}
		}
		fail( "REVCHANGES table not found" );
	}

	@Test
	public void testTrackAddedEntities(SessionFactoryScope scope) {
		StrTestEntity ste = new StrTestEntity( "x", steId );
		StrIntTestEntity site = new StrIntTestEntity( "y", 1, siteId );

		scope.inSession( em -> {
			assertTrue( TestTools.checkCollection(
					getCrossTypeRevisionChangesReader( em ).findEntities( 1 ),
					ste,
					site
			) );
		} );
	}

	@Test
	public void testTrackModifiedEntities(SessionFactoryScope scope) {
		StrIntTestEntity site = new StrIntTestEntity( "y", 2, siteId );

		scope.inSession( em -> {
			assertTrue( TestTools.checkCollection(
					getCrossTypeRevisionChangesReader( em ).findEntities( 2 ),
					site
			) );
		} );
	}

	@Test
	public void testTrackDeletedEntities(SessionFactoryScope scope) {
		StrTestEntity ste = new StrTestEntity( null, steId );
		StrIntTestEntity site = new StrIntTestEntity( null, null, siteId );

		scope.inSession( em -> {
			assertTrue( TestTools.checkCollection(
					getCrossTypeRevisionChangesReader( em ).findEntities( 3 ),
					site,
					ste
			) );
		} );
	}

	@Test
	public void testFindChangesInInvalidRevision(SessionFactoryScope scope) {
		scope.inSession( em -> {
			assertTrue( getCrossTypeRevisionChangesReader( em ).findEntities( 4 ).isEmpty() );
		} );
	}

	@Test
	public void testTrackAddedEntitiesGroupByRevisionType(SessionFactoryScope scope) {
		StrTestEntity ste = new StrTestEntity( "x", steId );
		StrIntTestEntity site = new StrIntTestEntity( "y", 1, siteId );

		scope.inSession( em -> {
			Map<RevisionType, List<Object>> result = getCrossTypeRevisionChangesReader( em )
					.findEntitiesGroupByRevisionType( 1 );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.ADD ), site, ste ) );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.MOD ) ) );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.DEL ) ) );
		} );
	}

	@Test
	public void testTrackModifiedEntitiesGroupByRevisionType(SessionFactoryScope scope) {
		StrIntTestEntity site = new StrIntTestEntity( "y", 2, siteId );

		scope.inSession( em -> {
			Map<RevisionType, List<Object>> result = getCrossTypeRevisionChangesReader( em )
					.findEntitiesGroupByRevisionType( 2 );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.ADD ) ) );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.MOD ), site ) );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.DEL ) ) );
		} );
	}

	@Test
	public void testTrackDeletedEntitiesGroupByRevisionType(SessionFactoryScope scope) {
		StrTestEntity ste = new StrTestEntity( null, steId );
		StrIntTestEntity site = new StrIntTestEntity( null, null, siteId );

		scope.inSession( em -> {
			Map<RevisionType, List<Object>> result = getCrossTypeRevisionChangesReader( em )
					.findEntitiesGroupByRevisionType( 3 );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.ADD ) ) );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.MOD ) ) );
			assertTrue( TestTools.checkCollection( result.get( RevisionType.DEL ), site, ste ) );
		} );
	}

	@Test
	public void testFindChangedEntitiesByRevisionTypeADD(SessionFactoryScope scope) {
		StrTestEntity ste = new StrTestEntity( "x", steId );
		StrIntTestEntity site = new StrIntTestEntity( "y", 1, siteId );

		scope.inSession( em -> {
			assertTrue( TestTools.checkCollection(
					getCrossTypeRevisionChangesReader( em ).findEntities( 1, RevisionType.ADD ),
					ste,
					site
			) );
		} );
	}

	@Test
	public void testFindChangedEntitiesByRevisionTypeMOD(SessionFactoryScope scope) {
		StrIntTestEntity site = new StrIntTestEntity( "y", 2, siteId );

		scope.inSession( em -> {
			assertTrue( TestTools.checkCollection(
					getCrossTypeRevisionChangesReader( em ).findEntities( 2, RevisionType.MOD ),
					site
			) );
		} );
	}

	@Test
	public void testFindChangedEntitiesByRevisionTypeDEL(SessionFactoryScope scope) {
		StrTestEntity ste = new StrTestEntity( null, steId );
		StrIntTestEntity site = new StrIntTestEntity( null, null, siteId );

		scope.inSession( em -> {
			assertTrue( TestTools.checkCollection(
					getCrossTypeRevisionChangesReader( em ).findEntities( 3, RevisionType.DEL ),
					ste,
					site
			) );
		} );
	}

	@Test
	public void testFindEntityTypesChangedInRevision(SessionFactoryScope scope) {
		scope.inSession( em -> {
			assertEquals(
					TestTools.makeSet(
							Pair.make( StrTestEntity.class.getName(), StrTestEntity.class ),
							Pair.make( StrIntTestEntity.class.getName(), StrIntTestEntity.class )
					),
					getCrossTypeRevisionChangesReader( em ).findEntityTypes( 1 )
			);

			assertEquals(
					TestTools.makeSet( Pair.make( StrIntTestEntity.class.getName(), StrIntTestEntity.class ) ),
					getCrossTypeRevisionChangesReader( em ).findEntityTypes( 2 )
			);

			assertEquals(
					TestTools.makeSet(
							Pair.make( StrTestEntity.class.getName(), StrTestEntity.class ),
							Pair.make( StrIntTestEntity.class.getName(), StrIntTestEntity.class )
					),
					getCrossTypeRevisionChangesReader( em ).findEntityTypes( 3 )
			);
		} );
	}

	private CrossTypeRevisionChangesReader getCrossTypeRevisionChangesReader(jakarta.persistence.EntityManager em) {
		return AuditReaderFactory.get( em ).getCrossTypeRevisionChangesReader();
	}
}
