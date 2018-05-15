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
import org.hibernate.envers.test.EnversSessionFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicAuditedEntity;
import org.hibernate.envers.test.support.domains.basic.BasicNonAuditedEntity;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * Test that checks the behavior of the {@link org.hibernate.envers.AuditReader}.
 *
 * @author Chris Cranford
 */
public class AuditReaderTest extends EnversSessionFactoryBasedFunctionalTest {

	// todo (6.0) - add more api tests for other AuditReader methods.

	private Integer auditedId;
	private Integer nonAuditedId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicAuditedEntity.class, BasicNonAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity audited = new BasicAuditedEntity( "str1", 1L );
			final BasicNonAuditedEntity nonAudited = new BasicNonAuditedEntity( "str1", null );
			session.save( audited );
			session.save( nonAudited );

			auditedId = audited.getId();
			nonAuditedId = nonAudited.getId();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity audited = session.find( BasicAuditedEntity.class, auditedId );
			final BasicNonAuditedEntity nonAudited = session.find( BasicNonAuditedEntity.class, nonAuditedId );
			audited.setStr1( "str2" );
			nonAudited.setStr1( "str2" );
		} );

		doInHibernate( this::sessionFactory, session -> {
			final BasicAuditedEntity audited = session.find( BasicAuditedEntity.class, auditedId );
			session.remove( audited );
		} );
	}

	@DynamicTest
	public void testIsEntityClassAuditedForAuditedEntity() {
		assertThat( getAuditReader().isEntityClassAudited( BasicAuditedEntity.class ), is( true ) );
		assertThat( getAuditReader().getRevisions( BasicAuditedEntity.class, auditedId ), is( asList( 1, 2, 3 ) ) );
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

		assertThat( revisions.size(), is( 2 ) );
		revisions.forEach( r -> assertThat( r, instanceOf( SequenceIdRevisionEntity.class ) ) );
	}

	@DynamicTest
	@TestForIssue(jiraKey = "HHH-7555")
	public void testFindRevisionEntitiesWithDeletions() {
		final List<?> revisions = getAuditReader()
				.createQuery()
				.forRevisionsOfEntity( BasicAuditedEntity.class, true )
				.getResultList();

		assertThat( revisions.size(), is( 3 ) );
		revisions.forEach( r -> assertThat( r, instanceOf( SequenceIdRevisionEntity.class ) ) );
	}

	@DynamicTest(expected = NotAuditedException.class)
	@TestForIssue(jiraKey = "HHH-7555")
	public void testFindRevisionEnttiiesNonAuditedEntity() {
		getAuditReader().createQuery().forRevisionsOfEntity( BasicNonAuditedEntity.class, false ).getResultList();
	}
}
