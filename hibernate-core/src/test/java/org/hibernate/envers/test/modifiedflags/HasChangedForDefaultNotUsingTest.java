/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.List;

import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.components.Component1;
import org.hibernate.envers.test.support.domains.components.Component2;
import org.hibernate.envers.test.support.domains.modifiedflags.PartialModifiedFlagsEntity;
import org.hibernate.envers.test.support.domains.modifiedflags.WithModifiedFlagReferencingEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedForDefaultNotUsingTest extends AbstractModifiedFlagsEntityTest {
	private static final int entityId = 1;
	private static final int refEntityId = 1;

	@Override
	public boolean forceModifiedFlags() {
		return false;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				PartialModifiedFlagsEntity.class,
				WithModifiedFlagReferencingEntity.class,
				StrTestEntity.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				em -> {
					PartialModifiedFlagsEntity entity = new PartialModifiedFlagsEntity( entityId );

					// Revision 1
					em.getTransaction().begin();
					em.persist( entity );
					em.getTransaction().commit();

					// Revision 2
					em.getTransaction().begin();
					entity.setData( "data1" );
					entity = em.merge( entity );
					em.getTransaction().commit();

					// Revision 3
					em.getTransaction().begin();
					entity.setComp1( new Component1( "str1", "str2" ) );
					entity = em.merge( entity );
					em.getTransaction().commit();

					// Revision 4
					em.getTransaction().begin();
					entity.setComp2( new Component2( "str1", "str2" ) );
					entity = em.merge( entity );
					em.getTransaction().commit();

					// Revision 5
					em.getTransaction().begin();
					WithModifiedFlagReferencingEntity withModifiedFlagReferencingEntity =
							new WithModifiedFlagReferencingEntity( refEntityId, "first" );
					withModifiedFlagReferencingEntity.setReference( entity );
					em.persist( withModifiedFlagReferencingEntity );
					em.getTransaction().commit();

					// Revision 6
					em.getTransaction().begin();
					withModifiedFlagReferencingEntity = em.find( WithModifiedFlagReferencingEntity.class, refEntityId );
					withModifiedFlagReferencingEntity.setReference( null );
					withModifiedFlagReferencingEntity.setSecondReference( entity );
					em.merge( withModifiedFlagReferencingEntity );
					em.getTransaction().commit();

					// Revision 7
					em.getTransaction().begin();
					entity.getStringSet().add( "firstElement" );
					entity.getStringSet().add( "secondElement" );
					entity = em.merge( entity );
					em.getTransaction().commit();

					// Revision 8
					em.getTransaction().begin();
					entity.getStringSet().remove( "secondElement" );
					entity.getStringMap().put( "someKey", "someValue" );
					entity = em.merge( entity );
					em.getTransaction().commit();

					// Revision 9 - main entity doesn't change
					em.getTransaction().begin();
					StrTestEntity strTestEntity = new StrTestEntity( "first" );
					em.persist( strTestEntity );
					em.getTransaction().commit();

					// Revision 10
					em.getTransaction().begin();
					entity.getEntitiesSet().add( strTestEntity );
					entity = em.merge( entity );
					em.getTransaction().commit();

					// Revision 11
					em.getTransaction().begin();
					entity.getEntitiesSet().remove( strTestEntity );
					entity.getEntitiesMap().put( "someKey", strTestEntity );
					em.merge( entity );
					em.getTransaction().commit();

					// Revision 12 - main entity doesn't change
					em.getTransaction().begin();
					strTestEntity.setStr( "second" );
					em.merge( strTestEntity );
					em.getTransaction().commit();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat(
				getAuditReader().getRevisions( PartialModifiedFlagsEntity.class, entityId ),
				contains( 1, 2, 3, 4, 5, 6, 7, 8, 10, 11 )
		);
	}

	@DynamicTest
	public void testHasChangedData() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "data" );
		assertThat( extractRevisions( list ), contains( 2 ) );
	}

	@DynamicTest
	public void testHasChangedComp1() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "comp1" );
		assertThat( extractRevisions( list ), contains( 3 ) );
	}

	@DynamicTest(expected = IllegalArgumentException.class)
	@Disabled("NullPointerException is thrown in SQM - unable to resolvePathPart")
	public void testHasChangedComp2() {
		queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "comp2" );
	}

	@DynamicTest
	public void testHasChangedReferencing() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "referencing" );
		assertThat( extractRevisions( list ), contains( 5, 6 ) );
	}

	@DynamicTest(expected = IllegalArgumentException.class)
	@Disabled("NullPointerException is thrown in SQM - unable to resolvePathPart")
	public void testHasChangedReferencing2() {
		queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "referencing2" );
	}

	@DynamicTest
	public void testHasChangedStringSet() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "stringSet" );
		assertThat( extractRevisions( list ), contains( 1, 7, 8 ) );
	}

	@DynamicTest
	public void testHasChangedStringMap() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "stringMap" );
		assertThat( extractRevisions( list ), contains( 1, 8 ) );
	}

	@DynamicTest
	public void testHasChangedStringSetAndMap() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "stringSet", "stringMap" );
		assertThat( extractRevisions( list ), contains( 1, 8 ) );
	}

	@DynamicTest
	public void testHasChangedEntitiesSet() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "entitiesSet" );
		assertThat( extractRevisions( list ), contains( 1, 10, 11 ) );
	}

	@DynamicTest
	public void testHasChangedEntitiesMap() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "entitiesMap" );
		assertThat( extractRevisions( list ), contains( 1, 11 ) );
	}

	@DynamicTest
	public void testHasChangedEntitiesSetAndMap() {
		final List list = queryForPropertyHasChanged( PartialModifiedFlagsEntity.class, entityId, "entitiesSet", "entitiesMap" );
		assertThat( extractRevisions( list ), contains( 1, 11 ) );
	}
}