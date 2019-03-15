/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.revisionentity.LongRevNumberRevEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class LongRevNumberTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, LongRevNumberRevEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrTestEntity te = new StrTestEntity( "x" );
					entityManager.persist( te );
					id = te.getId();
				},

				// Revision 2
				entityManager -> {
					final StrTestEntity entity = entityManager.find( StrTestEntity.class, id );
					entity.setStr( "y" );
				}
		);
	}

	@DynamicTest
	@Disabled("BaseSqmToSqlAstConverter#visitInListPredicate() ClassCastException - should use toExpression() rather than (Expression)")
	public void testFindRevision() {
		final AuditReader reader = getAuditReader();
		assertThat( reader.findRevision( LongRevNumberRevEntity.class, 1L ).getCustomId(), equalTo( 1L ) );
		assertThat( reader.findRevision( LongRevNumberRevEntity.class, 2L ).getCustomId(), equalTo( 2L ) );
	}

	@DynamicTest
	@Disabled("BaseSqmToSqlAstConverter#visitInListPredicate() ClassCastException - should use toExpression() rather than (Expression)")
	public void testFindRevisions() {
		final AuditReader reader = getAuditReader();

		Set<Number> revNumbers = new HashSet<>();
		revNumbers.add( 1L );
		revNumbers.add( 2L );

		Map<Number, LongRevNumberRevEntity> revisionMap = reader.findRevisions( LongRevNumberRevEntity.class, revNumbers );
		assertThat( revisionMap.entrySet(), CollectionMatchers.hasSize( 2 ) );
		assertThat( revisionMap, hasEntry( 1L, reader.findRevision( LongRevNumberRevEntity.class, 1L ) ) );
		assertThat( revisionMap, hasEntry( 2L, reader.findRevision( LongRevNumberRevEntity.class, 2L ) ) );
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, id ), contains( 1L, 2L ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		StrTestEntity ver1 = new StrTestEntity( id, "x" );
		StrTestEntity ver2 = new StrTestEntity( id, "y" );

		assertThat( getAuditReader().find( StrTestEntity.class, id, 1L ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( StrTestEntity.class, id, 2L ), equalTo( ver2 ) );
	}
}
