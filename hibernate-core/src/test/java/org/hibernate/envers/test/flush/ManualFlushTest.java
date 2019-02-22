/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.flush;

import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("Manual FlushMode does not work properly.")
public class ManualFlushTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					// Set FlushMode
					entityManager.unwrap( Session.class ).setFlushMode( FlushMode.MANUAL );

					// Revision 1
					entityManager.getTransaction().begin();
					StrTestEntity entity = new StrTestEntity( "x" );
					entityManager.persist( entity );
					entityManager.flush();
					entityManager.getTransaction().commit();
					this.id = entity.getId();

					// No Revision - Change the data but apply no flush
					entityManager.getTransaction().begin();
					entity = entityManager.find( StrTestEntity.class, entity.getId() );
					entity.setStr( "y" );
					entityManager.getTransaction().commit();

					// Revision 2 - Only the first change should be saved
					entityManager.getTransaction().begin();
					entity = entityManager.find( StrTestEntity.class, entity.getId() );
					entity.setStr( "z" );
					entityManager.flush();
					entity = entityManager.find( StrTestEntity.class, entity.getId() );
					entity.setStr( "z2" );
					entityManager.getTransaction().commit();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id ), contains( 1, 2  ) );
	}

	@DynamicTest
	public void testHistoryOfId() {
		StrTestEntity ver1 = new StrTestEntity( id, "x" );
		StrTestEntity ver2 = new StrTestEntity( id, "z" );

		assertThat( getAuditReader().find( StrTestEntity.class, id, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( StrTestEntity.class, id, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testCurrent() {
		inTransaction(
				entityManager -> {
					final StrTestEntity entity = entityManager.find( StrTestEntity.class, id );
					assertThat( entity, equalTo( new StrTestEntity( id, "z" ) ) );
				}
		);
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testRevisionTypes() {
		List<Object[]> results = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrTestEntity.class, false, true )
				.add( AuditEntity.id().eq( id ) )
				.getResultList();

		assertThat( results, CollectionMatchers.hasSize( 2 ) );
		assertThat( results.get( 0 )[2], equalTo( RevisionType.ADD ) );
		assertThat( results.get( 1 )[2], equalTo( RevisionType.MOD ) );
	}
}
