/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.dynamic;

import java.util.Arrays;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.dynamic.DynamicMapComponentNotAuditedEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-8049")
public class DynamicMapComponentNotAuditedTest extends EnversSessionFactoryBasedFunctionalTest {
	private Long entityId;

	@Override
	protected String[] getMappings() {
		return new String[] { "dynamic-components/DynamicMapComponentNotAuditedEntity.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Revision 1
		sessionFactoryScope().inTransaction( session -> {
			DynamicMapComponentNotAuditedEntity entity = new DynamicMapComponentNotAuditedEntity( 1L, "static field value" );
			entity.getCustomFields().put( "prop1", 13 );
			entity.getCustomFields().put( "prop2", 0.1f );
			session.save( entity );
			this.entityId = entity.getId();
		} );

		// No revision
		sessionFactoryScope().inTransaction( session -> {
			DynamicMapComponentNotAuditedEntity entity = session.get( DynamicMapComponentNotAuditedEntity.class, entityId );
			entity.getCustomFields().put( "prop1", 0 );
			session.update( entity );
		} );

		// Revision 2
		sessionFactoryScope().inTransaction( session -> {
			DynamicMapComponentNotAuditedEntity entity = session.get( DynamicMapComponentNotAuditedEntity.class, entityId );
			entity.setNote( "updated note" );
			session.update( entity );
		} );

		// Revision 3
		sessionFactoryScope().inTransaction( session -> {
			session.delete( session.load( DynamicMapComponentNotAuditedEntity.class, entityId ) );
		} );
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat(
				getAuditReader().getRevisions( DynamicMapComponentNotAuditedEntity.class, 1L ),
				is( Arrays.asList( 1, 2, 3 ) )
		);
	}

	@DynamicTest
	public void testHistoryOfEntity1() {
		DynamicMapComponentNotAuditedEntity ver1 = new DynamicMapComponentNotAuditedEntity( 1L, "static field value" );
		DynamicMapComponentNotAuditedEntity ver2 = new DynamicMapComponentNotAuditedEntity( 1L, "updated note" );

		// NOTE: Custom fields is empty because it is not audited

		DynamicMapComponentNotAuditedEntity rev1 = getAuditReader().find( DynamicMapComponentNotAuditedEntity.class, 1L, 1 );
		assertThat( rev1, is( ver1 ) );
		assertThat( rev1.getCustomFields().isEmpty(), is( true ) );

		DynamicMapComponentNotAuditedEntity rev2 = getAuditReader().find( DynamicMapComponentNotAuditedEntity.class, 1L, 2 );
		assertThat( rev2, is( ver2 ) );
		assertThat( rev2.getCustomFields().isEmpty(), is( true ) );
	}
}
