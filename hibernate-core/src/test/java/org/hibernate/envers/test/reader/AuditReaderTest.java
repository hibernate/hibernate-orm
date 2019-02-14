/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.reader;

import java.util.List;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;
import org.hibernate.envers.test.support.domains.basic.BasicNonAuditedEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * Test that checks the behavior of the {@link org.hibernate.envers.AuditReader}.
 *
 * @author Chris Cranford
 */
public class AuditReaderTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	// todo (6.0) - add more api tests for other AuditReader methods.

	private Integer auditedId;
	private Integer nonAuditedId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class, BasicNonAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final BasicAuditedEntity audited = new BasicAuditedEntity( "str1", 1L );
					final BasicNonAuditedEntity nonAudited = new BasicNonAuditedEntity( "str1", null );
					entityManager.persist( audited );
					entityManager.persist( nonAudited );

					auditedId = audited.getId();
					nonAuditedId = nonAudited.getId();
				}
		);

		inTransaction(
				entityManager -> {
					final BasicAuditedEntity audited = entityManager.find( BasicAuditedEntity.class, auditedId );
					final BasicNonAuditedEntity nonAudited = entityManager.find( BasicNonAuditedEntity.class, nonAuditedId );
					audited.setStr1( "str2" );
					nonAudited.setStr1( "str2" );
				}
		);

		inTransaction(
				entityManager -> {
					final BasicAuditedEntity audited = entityManager.find( BasicAuditedEntity.class, auditedId );
					entityManager.remove( audited );
				}
		);
	}

	@DynamicTest
	public void testIsEntityClassAuditedForAuditedEntity() {
		assertThat( getAuditReader().isEntityClassAudited( BasicAuditedEntity.class ), is( true ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, auditedId ), hasItems( 1, 2, 3 ) );
	}

	@DynamicTest(expected = NotAuditedException.class)
	public void testIsEntityClassAuditedForNotAuditedEntity() {
		assertThat( getAuditReader().isEntityClassAudited( BasicNonAuditedEntity.class ), is( false ) );
		getAuditReader().getRevisions( BasicNonAuditedEntity.class, 1 );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7555")
	public void testFindRevisionEntitiesWithoutDeletions() {
		final List<?> revisions = getAuditReader()
				.createQuery()
				.forRevisionsOfEntity( BasicAuditedEntity.class, false )
				.getResultList();

		assertThat( revisions, hasSize( 2 ) );
		revisions.forEach( r -> assertThat( r, instanceOf( SequenceIdRevisionEntity.class ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7555")
	public void testFindRevisionEntitiesWithDeletions() {
		final List<?> revisions = getAuditReader()
				.createQuery()
				.forRevisionsOfEntity( BasicAuditedEntity.class, true )
				.getResultList();

		assertThat( revisions, hasSize( 3 ) );
		revisions.forEach( r -> assertThat( r, instanceOf( SequenceIdRevisionEntity.class ) ) );
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-7555")
	public void testFindRevisionEnttiiesNonAuditedEntity() {
		getAuditReader().createQuery().forRevisionsOfEntity( BasicNonAuditedEntity.class, false ).getResultList();
	}
}
