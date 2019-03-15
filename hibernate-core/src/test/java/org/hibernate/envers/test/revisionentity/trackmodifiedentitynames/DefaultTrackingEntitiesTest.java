/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity.trackmodifiedentitynames;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.tools.Pair;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests proper behavior of tracking modified entity names when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings({"unchecked"})
public class DefaultTrackingEntitiesTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer steId = null;
	private Integer siteId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, StrIntTestEntity.class };
	}

	@Override
	public void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.TRACK_ENTITIES_CHANGED_IN_REVISION, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 - Adding two entities
				entityManager -> {
					final StrTestEntity ste = new StrTestEntity( "x" );
					final StrIntTestEntity site = new StrIntTestEntity( "y", 1 );

					entityManager.persist( ste );
					entityManager.persist( site );

					steId = ste.getId();
					siteId = site.getId();
				},

				// Revision 2 - Modifying one entity
				entityManager -> {
					final StrIntTestEntity site = entityManager.find( StrIntTestEntity.class, siteId );
					site.setNumber( 2 );
				},

				// Revision 3 - Deleting both entities
				entityManager -> {
					final StrTestEntity ste = entityManager.find( StrTestEntity.class, steId );
					final StrIntTestEntity site = entityManager.find( StrIntTestEntity.class, siteId );
					entityManager.remove( ste );
					entityManager.remove( site );
				}
		);
	}

	@DynamicTest
	public void testRevEntityTableCreation() {
		final RevisionChangesInspector inspector = new RevisionChangesInspector();
		getMetamodel().visitCollectionDescriptors( inspector );
		assertThat( inspector.isLocated(), equalTo( true ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackAddedEntities() {
		final StrTestEntity ste = new StrTestEntity( steId, "x" );
		final StrIntTestEntity site = new StrIntTestEntity( "y", 1, siteId );
		assertThat( getCrossTypeRevisionReader().findEntities( 1 ), containsInAnyOrder( ste, site ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackModifiedEntities() {
		StrIntTestEntity site = new StrIntTestEntity( "y", 2, siteId );
		assertThat( getCrossTypeRevisionReader().findEntities( 2 ), containsInAnyOrder( site ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackDeletedEntities() {
		StrTestEntity ste = new StrTestEntity( steId, null );
		StrIntTestEntity site = new StrIntTestEntity( null, null, siteId );
		assertThat( getCrossTypeRevisionReader().findEntities( 3 ), containsInAnyOrder( site, ste ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testFindChangesInInvalidRevision() {
		assertThat( getCrossTypeRevisionReader().findEntities( 4 ), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackAddedEntitiesGroupByRevisionType() {
		StrTestEntity ste = new StrTestEntity( steId, "x" );
		StrIntTestEntity site = new StrIntTestEntity( "y", 1, siteId );

		Map<RevisionType, List<Object>> result = getCrossTypeRevisionReader().findEntitiesGroupByRevisionType( 1 );
		assertThat( result.get( RevisionType.ADD ), containsInAnyOrder( site, ste ) );
		assertThat( result.get( RevisionType.MOD ), CollectionMatchers.isEmpty() );
		assertThat( result.get( RevisionType.DEL ), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackModifiedEntitiesGroupByRevisionType() {
		StrIntTestEntity site = new StrIntTestEntity( "y", 2, siteId );

		Map<RevisionType, List<Object>> result = getCrossTypeRevisionReader().findEntitiesGroupByRevisionType( 2 );
		assertThat( result.get( RevisionType.ADD ), CollectionMatchers.isEmpty() );
		assertThat( result.get( RevisionType.MOD ), containsInAnyOrder( site ) );
		assertThat( result.get( RevisionType.DEL ), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testTrackDeletedEntitiesGroupByRevisionType() {
		StrTestEntity ste = new StrTestEntity( steId, null );
		StrIntTestEntity site = new StrIntTestEntity( null, null, siteId );

		Map<RevisionType, List<Object>> result = getCrossTypeRevisionReader().findEntitiesGroupByRevisionType( 3 );
		assertThat( result.get( RevisionType.ADD ), CollectionMatchers.isEmpty() );
		assertThat( result.get( RevisionType.MOD ), CollectionMatchers.isEmpty() );
		assertThat( result.get( RevisionType.DEL ), containsInAnyOrder( site, ste ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testFindChangedEntitiesByRevisionTypeADD() {
		StrTestEntity ste = new StrTestEntity( steId, "x" );
		StrIntTestEntity site = new StrIntTestEntity( "y", 1, siteId );

		assertThat( getCrossTypeRevisionReader().findEntities( 1, RevisionType.ADD ), containsInAnyOrder( ste, site ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testFindChangedEntitiesByRevisionTypeMOD() {
		StrIntTestEntity site = new StrIntTestEntity( "y", 2, siteId );

		assertThat( getCrossTypeRevisionReader().findEntities( 2, RevisionType.MOD ), containsInAnyOrder( site ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testFindChangedEntitiesByRevisionTypeDEL() {
		StrTestEntity ste = new StrTestEntity( steId, null );
		StrIntTestEntity site = new StrIntTestEntity( null, null, siteId );

		assertThat( getCrossTypeRevisionReader().findEntities( 3, RevisionType.DEL ), containsInAnyOrder( ste, site ) );
	}

	@DynamicTest
	@Disabled("NYI - IllegalStateException thrown when trying to bind multi-values")
	public void testFindEntityTypesChangedInRevision() {
		assertThat(
				getCrossTypeRevisionReader().findEntityTypes( 1 ),
				containsInAnyOrder(
						Pair.make( StrTestEntity.class.getName(), StrTestEntity.class ),
						Pair.make( StrIntTestEntity.class.getName(), StrIntTestEntity.class )
				)
		);

		assertThat(
				getCrossTypeRevisionReader().findEntityTypes( 2 ),
				containsInAnyOrder(
						Pair.make( StrIntTestEntity.class.getName(), StrIntTestEntity.class )
				)
		);

		assertThat(
				getCrossTypeRevisionReader().findEntityTypes( 3 ),
				containsInAnyOrder(
						Pair.make( StrTestEntity.class.getName(), StrTestEntity.class ),
						Pair.make( StrIntTestEntity.class.getName(), StrIntTestEntity.class )
				)
		);
	}

	private CrossTypeRevisionChangesReader getCrossTypeRevisionReader() {
		return getAuditReader().getCrossTypeRevisionChangesReader();
	}

	class RevisionChangesInspector implements Consumer<PersistentCollectionDescriptor<?,?,?>> {
		private boolean located;
		@Override
		public void accept(PersistentCollectionDescriptor<?,?,?> collectionDescriptor) {
			final Table collectionTable = collectionDescriptor.getSeparateCollectionTable();
			if ( collectionTable != null && collectionTable.getTableExpression().equals( "REVCHANGES" ) ) {
				assertThat( located, equalTo( false ) );
				located = true;

				assertThat( collectionTable.getColumns(), CollectionMatchers.hasSize( 2 ) );
				assertThat( collectionTable.getColumn( "REV" ), notNullValue() );
				assertThat( collectionTable.getColumn( "ENTITYNAME" ), notNullValue() );
			}
		}

		public boolean isLocated() {
			return located;
		}
	}
}
