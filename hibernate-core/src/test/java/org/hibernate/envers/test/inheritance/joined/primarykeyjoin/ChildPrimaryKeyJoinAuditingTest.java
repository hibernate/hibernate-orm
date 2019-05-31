/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.inheritance.joined.primarykeyjoin;

import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.inheritance.joined.ParentEntity;
import org.hibernate.envers.test.support.domains.inheritance.joined.primarykeyjoin.ChildPrimaryKeyJoinEntity;
import org.hibernate.mapping.Column;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Joined Inheritance Support")
public class ChildPrimaryKeyJoinAuditingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ChildPrimaryKeyJoinEntity.class, ParentEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		id1 = 1;

		inTransactions(
				// Revision 1
				entityManager -> {
					ChildPrimaryKeyJoinEntity ce = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
					entityManager.persist( ce );
				},

				// Revision 2
				entityManager -> {
					ChildPrimaryKeyJoinEntity ce = entityManager.find( ChildPrimaryKeyJoinEntity.class, id1 );
					ce.setData( "y" );
					ce.setNumVal( 2l );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ChildPrimaryKeyJoinEntity.class, id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfChildId1() {
		ChildPrimaryKeyJoinEntity ver1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );
		ChildPrimaryKeyJoinEntity ver2 = new ChildPrimaryKeyJoinEntity( id1, "y", 2l );

		assertThat( getAuditReader().find( ChildPrimaryKeyJoinEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ChildPrimaryKeyJoinEntity.class, id1, 2 ), equalTo( ver2 ) );

		assertThat( getAuditReader().find( ParentEntity.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( ParentEntity.class, id1, 2 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testPolymorphicQuery() {
		ChildPrimaryKeyJoinEntity childVer1 = new ChildPrimaryKeyJoinEntity( id1, "x", 1l );

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( ChildPrimaryKeyJoinEntity.class, 1 ).getSingleResult(),
				equalTo( childVer1 )
		);

		assertThat(
				getAuditReader().createQuery().forEntitiesAtRevision( ParentEntity.class, 1 ).getSingleResult(),
				equalTo( childVer1 )
		);
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testChildIdColumnName() {
		List<Column> idColumns = getAuditEntityDescriptor( ChildPrimaryKeyJoinEntity.class ).getIdentifierDescriptor().getColumns();
		assertThat( idColumns.iterator().next().getName(), equalTo( "other_id" ) );
	}
}