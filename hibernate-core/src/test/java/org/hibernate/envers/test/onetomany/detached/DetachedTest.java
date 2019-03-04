/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.detached;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.onetomany.detached.ListRefCollEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7543")
public class DetachedTest extends EnversSessionFactoryBasedFunctionalTest {
	private Integer parentId = null;
	private Integer childId = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ListRefCollEntity.class, StrTestEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		//		This is currently a workaround to get the test to pass.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@DynamicBeforeAll
	public void testUpdatingDetachedEntityWithRelation() {
		// Revision 1
		final ListRefCollEntity detachedParent = inTransaction(
				session -> {
					ListRefCollEntity parent = new ListRefCollEntity( 1, "initial data" );
					StrTestEntity child = new StrTestEntity( "data" );
					session.save( child );
					parent.setCollection( Arrays.asList( child ) );
					session.save( parent );

					this.parentId = parent.getId();
					this.childId = child.getId();

					return parent;
				}
		);

		// Revision 2 - updating detached entity
		inTransaction(
				session -> {
					detachedParent.setData( "modified data" );
					session.update ( detachedParent );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ListRefCollEntity.class, parentId ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, childId ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfParent() {
		final ListRefCollEntity parent = new ListRefCollEntity( parentId, "initial data" );
		parent.setCollection( Arrays.asList( new StrTestEntity( childId, "data" ) ) );

		final ListRefCollEntity ver1 = getAuditReader().find( ListRefCollEntity.class, parentId, 1 );
		assertThat( ver1, equalTo( parent ) );
		assertThat( ver1.getCollection(), equalTo( parent.getCollection() ) );

		parent.setData( "modified data" );

		final ListRefCollEntity ver2 = getAuditReader().find( ListRefCollEntity.class, parentId, 2 );
		assertThat( ver2, equalTo( parent ) );
		assertThat( ver2.getCollection(), equalTo( parent.getCollection() ) );
	}
}