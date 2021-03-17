/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.auditReader;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntNoAutoIdTestEntity;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static junit.framework.TestCase.assertNull;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * A test which verifies the behavior of the various {@link AuditReader} find implementations when the
 * configuration option {@link EnversSettings#FIND_BY_REVISION_EXACT_MATCH} is enabled.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-13500")
public class FindByRevisionExactMatchOptionTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );

		options.put( EnversSettings.FIND_BY_REVISION_EXACT_MATCH, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IntNoAutoIdTestEntity.class };
	}

	@Priority(10)
	@Test
	public void initData() {
		// Insert entity with id=1, numVal=1, revision 1
		doInJPA( this::entityManagerFactory, entityManager -> {
			final IntNoAutoIdTestEntity entity = new IntNoAutoIdTestEntity( 1, 1 );
			entityManager.persist( entity );
		} );

		// Update entity with id=1, setting numVal=11, revision 2
		doInJPA( this::entityManagerFactory, entityManager -> {
			final IntNoAutoIdTestEntity entity = entityManager.find( IntNoAutoIdTestEntity.class, 1 );
			entity.setNumVal( 11 );
			entityManager.merge( entity );
		} );

		// Insert entity with id=2, numVal=2, revision 3
		doInJPA( this::entityManagerFactory, entityManager -> {
			final IntNoAutoIdTestEntity entity = new IntNoAutoIdTestEntity( 2, 2 );
			entityManager.persist( entity );
		} );

		// Update entity with id=2, setting numVal=22, revision 4
		doInJPA( this::entityManagerFactory, entityManager -> {
			final IntNoAutoIdTestEntity entity = entityManager.find( IntNoAutoIdTestEntity.class, 2 );
			entity.setNumVal( 22 );
			entityManager.merge( entity );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, 1 ) );
		assertEquals( Arrays.asList( 3, 4 ), getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, 2 ) );
	}

	@Test
	public void testFindEntityId1() {
		final AuditReader auditReader = getAuditReader();
		assertEquals( new IntNoAutoIdTestEntity( 1, 1 ), auditReader.find( IntNoAutoIdTestEntity.class, 1, 1 ) );
		assertEquals( new IntNoAutoIdTestEntity( 11, 1 ), auditReader.find( IntNoAutoIdTestEntity.class, 1, 2 ) );
		assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 1, 3 ) );
		assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 1, 4 ) );
	}

	@Test
	public void testFindEntityId2() {
		final AuditReader auditReader = getAuditReader();
		assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 2, 1 ) );
		assertNull( auditReader.find( IntNoAutoIdTestEntity.class, 2, 2 ) );
		assertEquals( new IntNoAutoIdTestEntity( 2, 2 ), auditReader.find( IntNoAutoIdTestEntity.class, 2, 3 ) );
		assertEquals( new IntNoAutoIdTestEntity( 22, 2 ), auditReader.find( IntNoAutoIdTestEntity.class, 2, 4 ) );
	}
}
