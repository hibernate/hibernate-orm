/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7246")
public class EmptyStringTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					StrTestEntity e1 = new StrTestEntity( "" );
					entityManager.persist( e1 );

					StrTestEntity e2 = new StrTestEntity( null );
					entityManager.persist( e2 );

					this.id1 = e1.getId();
					this.id2 = e2.getId();
				}
		);

		// Should not generate a revision after NULL to "" modification or vice versa on Oracle
		inTransaction(
				entityManager -> {
					StrTestEntity e1 = entityManager.find( StrTestEntity.class, this.id1 );
					e1.setStr( null );
					entityManager.merge( e1 );

					StrTestEntity e2 = entityManager.find( StrTestEntity.class, this.id2 );
					e2.setStr( "" );
					entityManager.merge( e2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		final List<Number> expectedRevisions = resolveExpectedRevisions();
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, this.id1 ), is( expectedRevisions ) );
		assertThat( getAuditReader().getRevisions( StrTestEntity.class, this.id2 ), is( expectedRevisions ) );
	}

	private List<Number> resolveExpectedRevisions() {
		if ( getDialect().isEmptyStringTreatedAsNull() ) {
			return Collections.singletonList( 1 );
		}
		return Arrays.asList( 1, 2 );
	}
}
