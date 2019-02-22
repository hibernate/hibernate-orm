/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.strategy;

import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntNoAutoIdTestEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests that reusing identifiers doesn't cause auditing misbehavior.
 *
 * @author adar
 */
@TestForIssue(jiraKey = "HHH-8280")
public class IdentifierReuseTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private static final Integer REUSED_IDENTIFIER = 1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IntNoAutoIdTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );
		settings.put( EnversSettings.ALLOW_IDENTIFIER_REUSE, "true" );
	}

	@DynamicTest
	public void testIdentifierReuse() {
		// Revision 1-3 persist, modify, and remove an entity with id 1
		// Revision 4-6 persist, modify, and remove a new entity that reuses the same id 1.
		inTransactions(
				// Revision 1
				this::persist,
				// Revision 2
				this::fetchModifyAndMerge,
				// Revision 3
				this::fetchAndRemove,
				// Revision 4
				this::persist,
				// Revision 5
				this::fetchModifyAndMerge,
				// Revision 6
				this::fetchAndRemove
		);

		assertThat(
				getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, REUSED_IDENTIFIER ),
				contains( 1, 2, 3, 4, 5, 6 )
		);
	}

	private void persist(EntityManager entityManager) {
		IntNoAutoIdTestEntity entity = new IntNoAutoIdTestEntity( 0, REUSED_IDENTIFIER );
		entityManager.persist( entity );
		assertThat( entity.getId(), equalTo( REUSED_IDENTIFIER ) );
	}

	private void fetchModifyAndMerge(EntityManager entityManager) {
		IntNoAutoIdTestEntity entity = entityManager.find( IntNoAutoIdTestEntity.class, REUSED_IDENTIFIER );
		entity.setNumVal( 1 );
		entity = entityManager.merge( entity );
		assertThat( entity.getId(), equalTo( REUSED_IDENTIFIER ) );
	}

	private void fetchAndRemove(EntityManager entityManager) {
		IntNoAutoIdTestEntity entity = entityManager.find( IntNoAutoIdTestEntity.class, REUSED_IDENTIFIER );
		assertThat( entity, notNullValue() );
		entityManager.remove( entity );
	}
}
