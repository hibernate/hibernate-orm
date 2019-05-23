/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.components.dynamic;

import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.components.dynamic.NotAuditedDynamicMapComponent;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-8049")
public class NotAuditedDynamicComponentTest extends EnversSessionFactoryBasedFunctionalTest {
	private Long id;

	@Override
	protected String[] getMappings() {
		return new String[] { "dynamic-components/MapNotAudited.hbm.xml" };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				session -> {
					NotAuditedDynamicMapComponent entity = new NotAuditedDynamicMapComponent( 1L, "static field value" );
					entity.getCustomFields().put( "prop1", 13 );
					entity.getCustomFields().put( "prop2", 0.1f );
					session.save( entity );

					this.id = entity.getId();
				},

				// No Revision
				session -> {
					NotAuditedDynamicMapComponent entity = session.get( NotAuditedDynamicMapComponent.class, id );
					entity.getCustomFields().put( "prop1", 0 );
					session.update( entity );
				},

				// Revision 2
				session -> {
					NotAuditedDynamicMapComponent entity = session.get( NotAuditedDynamicMapComponent.class, id );
					entity.setNote( "updated note" );
					session.update( entity );
				},

				// Revision 3
				session -> {
					NotAuditedDynamicMapComponent entity = session.load( NotAuditedDynamicMapComponent.class, id );
					session.delete( entity );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( NotAuditedDynamicMapComponent.class, id ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		// Revision 1
		final NotAuditedDynamicMapComponent ver1 = getAuditReader().find( NotAuditedDynamicMapComponent.class, id, 1 );
		assertThat( ver1, equalTo( new NotAuditedDynamicMapComponent( id, "static field value" ) ) );
		// Assume empty NotAuditedDynamicMapComponent#customFields map, because dynamic-component is not audited.
		assertThat( ver1.getCustomFields().entrySet(), CollectionMatchers.isEmpty() );

		// Revision 2
		final NotAuditedDynamicMapComponent ver2 = getAuditReader().find( NotAuditedDynamicMapComponent.class, id, 2 );
		assertThat( ver2, equalTo( new NotAuditedDynamicMapComponent( id, "updated note" ) ) );
		// Assume empty NotAuditedDynamicMapComponent#customFields map, because dynamic-component is not audited.
		assertThat( ver2.getCustomFields().entrySet(), CollectionMatchers.isEmpty() );
	}
}
