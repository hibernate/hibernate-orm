/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytoone.bidirectional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytoone.bidirectional.ManyToOneOwning;
import org.hibernate.envers.test.support.domains.manytoone.bidirectional.OneToManyOwned;
import org.hibernate.mapping.ManyToOne;
import org.junit.Assert;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-4962")
public class ImplicitMappedByTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long ownedId = null;
	private Long owning1Id = null;
	private Long owning2Id = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OneToManyOwned.class, ManyToOneOwning.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final OneToManyOwned owned = new OneToManyOwned( "data", null );
					Set<ManyToOneOwning> referencing = new HashSet<>();

					final ManyToOneOwning owning1 = new ManyToOneOwning( "data1", owned );
					referencing.add( owning1 );

					final ManyToOneOwning owning2 = new ManyToOneOwning( "data2", owned );
					referencing.add( owning2 );

					owned.setReferencing( referencing );

					entityManager.persist( owned );
					entityManager.persist( owning1 );
					entityManager.persist( owning2 );

					ownedId = owned.getId();
					owning1Id = owning1.getId();
					owning2Id = owning2.getId();
				},

				// Revision 2
				entityManager -> {
					final ManyToOneOwning owning1 = entityManager.find( ManyToOneOwning.class, owning1Id );
					entityManager.remove( owning1 );
				},

				// Revision 3
				entityManager -> {
					final ManyToOneOwning owning2 = entityManager.find( ManyToOneOwning.class, owning2Id );
					owning2.setData( "data2modified" );
					entityManager.merge( owning2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( OneToManyOwned.class, ownedId ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( ManyToOneOwning.class, owning1Id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( ManyToOneOwning.class, owning2Id ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfOwned() {
		OneToManyOwned owned = new OneToManyOwned( "data", null, ownedId );
		ManyToOneOwning owning1 = new ManyToOneOwning( "data1", owned, owning1Id );
		ManyToOneOwning owning2 = new ManyToOneOwning( "data2", owned, owning2Id );

		OneToManyOwned ver1 = getAuditReader().find( OneToManyOwned.class, ownedId, 1 );
		assertThat( ver1, equalTo( owned ) );
		assertThat( ver1.getReferencing(), contains( owning1, owning2 ) );

		OneToManyOwned ver2 = getAuditReader().find( OneToManyOwned.class, ownedId, 2 );
		assertThat( ver2, equalTo( owned ) );
		assertThat( ver2.getReferencing(), contains( owning2 ) );
	}

	@DynamicTest
	public void testHistoryOfOwning1() {
		ManyToOneOwning ver1 = new ManyToOneOwning( "data1", null, owning1Id );
		assertThat( getAuditReader().find( ManyToOneOwning.class, owning1Id, 1 ), equalTo( ver1 ) );
	}

	@DynamicTest
	public void testHistoryOfOwning2() {
		OneToManyOwned owned = new OneToManyOwned( "data", null, ownedId );
		ManyToOneOwning owning1 = new ManyToOneOwning( "data2", owned, owning2Id );
		ManyToOneOwning owning3 = new ManyToOneOwning( "data2modified", owned, owning2Id );

		ManyToOneOwning ver1 = getAuditReader().find( ManyToOneOwning.class, owning2Id, 1 );
		ManyToOneOwning ver3 = getAuditReader().find( ManyToOneOwning.class, owning2Id, 3 );

		assertThat( owning1, equalTo( ver1 ) );
		assertThat( owned.getId(), equalTo( ver1.getReferences().getId() ) );
		assertThat( owning3, equalTo( ver3 ) );
		assertThat( owned.getId(), equalTo( ver3.getReferences().getId() ) );
	}
}