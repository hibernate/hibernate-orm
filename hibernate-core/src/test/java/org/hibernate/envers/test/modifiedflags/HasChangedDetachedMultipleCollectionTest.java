/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.test.support.domains.collections.MultipleCollectionEntity;
import org.hibernate.envers.test.support.domains.collections.MultipleCollectionRefEntity1;
import org.hibernate.envers.test.support.domains.collections.MultipleCollectionRefEntity2;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7437")
@SkipForDialect(value = Oracle8iDialect.class, comment = "Oracle does not support identity key generation")
@Disabled("@Version column throws PropertyValueException due to transient value")
public class HasChangedDetachedMultipleCollectionTest extends AbstractModifiedFlagsEntityTest {
	private Long mce1Id = null;
	private Long mce2Id = null;
	private Long mcre1Id = null;
	private Long mcre2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MultipleCollectionEntity.class,
				MultipleCollectionRefEntity1.class,
				MultipleCollectionRefEntity2.class
		};
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		EntityManager entityManager = getOrCreateEntityManager();
		try {
			// Revision 1 - addition.
			entityManager.getTransaction().begin();
			MultipleCollectionEntity mce1 = new MultipleCollectionEntity();
			mce1.setText( "MultipleCollectionEntity-1-1" );
			// Persisting entity with empty collections.
			entityManager.persist( mce1 );
			entityManager.getTransaction().commit();

			mce1Id = mce1.getId();

			// Revision 2 - update.
			entityManager.getTransaction().begin();
			mce1 = entityManager.find( MultipleCollectionEntity.class, mce1.getId() );
			MultipleCollectionRefEntity1 mcre1 = new MultipleCollectionRefEntity1();
			mcre1.setText( "MultipleCollectionRefEntity1-1-1" );
			mcre1.setMultipleCollectionEntity( mce1 );
			mce1.addRefEntity1( mcre1 );
			entityManager.persist( mcre1 );
			mce1 = entityManager.merge( mce1 );
			entityManager.getTransaction().commit();

			mcre1Id = mcre1.getId();

			// No changes.
			entityManager.getTransaction().begin();
			mce1 = entityManager.find( MultipleCollectionEntity.class, mce1.getId() );
			mce1 = entityManager.merge( mce1 );
			entityManager.getTransaction().commit();

			entityManager.close();

			entityManager = getOrCreateEntityManager();

			// Revision 3 - updating detached collection.
			entityManager.getTransaction().begin();
			mce1.removeRefEntity1( mcre1 );
			mce1 = entityManager.merge( mce1 );
			entityManager.getTransaction().commit();

			entityManager.close();

			entityManager = getOrCreateEntityManager();

			// Revision 4 - updating detached entity, no changes to collection attributes.
			entityManager.getTransaction().begin();
			mce1.setRefEntities1( new ArrayList<>() );
			mce1.setRefEntities2( new ArrayList<>() );
			mce1.setText( "MultipleCollectionEntity-1-2" );
			mce1 = entityManager.merge( mce1 );
			entityManager.getTransaction().commit();

			entityManager.close();

			entityManager = getOrCreateEntityManager();

			// No changes to detached entity (collections were empty before).
			entityManager.getTransaction().begin();
			mce1.setRefEntities1( new ArrayList<>() );
			mce1.setRefEntities2( new ArrayList<>() );
			mce1 = entityManager.merge( mce1 );
			entityManager.getTransaction().commit();

			// Revision 5 - addition.
			entityManager.getTransaction().begin();
			MultipleCollectionEntity mce2 = new MultipleCollectionEntity();
			mce2.setText( "MultipleCollectionEntity-2-1" );
			MultipleCollectionRefEntity2 mcre2 = new MultipleCollectionRefEntity2();
			mcre2.setText( "MultipleCollectionRefEntity2-1-1" );
			mcre2.setMultipleCollectionEntity( mce2 );
			mce2.addRefEntity2( mcre2 );
			// Cascade persisting related entity.
			entityManager.persist( mce2 );
			entityManager.getTransaction().commit();

			mce2Id = mce2.getId();
			mcre2Id = mcre2.getId();

		}
		catch( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@DynamicTest
	public void testHasChanged() {
		List list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce1Id, "text" );
		assertThat( extractRevisions( list ), contains( 1, 4 ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce1Id, "refEntities1" );
		assertThat( extractRevisions( list ), contains( 1, 2, 3 ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce1Id, "refEntities2" );
		assertThat( extractRevisions( list ), contains( 1 ) );

		list = queryForPropertyHasChanged( MultipleCollectionRefEntity1.class, mcre1Id, "text" );
		assertThat( extractRevisions( list ), contains( 2 ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce2Id, "text" );
		assertThat( extractRevisions( list ), contains( 5 ) );

		list = queryForPropertyHasChanged( MultipleCollectionEntity.class, mce2Id, "refEntities2" );
		assertThat( extractRevisions( list ), contains( 5 ) );

		list = queryForPropertyHasChanged( MultipleCollectionRefEntity2.class, mcre2Id, "text" );
		assertThat( extractRevisions( list ), contains( 5 ) );
	}
}