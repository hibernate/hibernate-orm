/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.onetomany.idclass;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.onetomany.idclass.ManyToManyCompositeKey;
import org.hibernate.envers.test.support.domains.onetomany.idclass.ManyToOneOwned;
import org.hibernate.envers.test.support.domains.onetomany.idclass.OneToManyOwned;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7625")
@Disabled("ClassCastException - EntityJavaDescriptorImpl->EmbeddableJavaDescriptor in EmbeddedJavaDescriptorImpl#resolveJtd")
public class OneToManyCompositeKeyTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private ManyToManyCompositeKey.ManyToManyId owning1Id = null;
	private ManyToManyCompositeKey.ManyToManyId owning2Id = null;
	private Long oneToManyId;
	private Long manyToOne1Id;
	private Long manyToOne2Id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OneToManyOwned.class, ManyToManyCompositeKey.class, ManyToOneOwned.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// Revision 1
		inTransaction(
				entityManager -> {
					OneToManyOwned oneToManyOwned = new OneToManyOwned( "data", null );
					ManyToOneOwned manyToOneOwned1 = new ManyToOneOwned( "data1" );
					ManyToOneOwned manyToOneOwned2 = new ManyToOneOwned( "data2" );
					ManyToManyCompositeKey owning1 = new ManyToManyCompositeKey( oneToManyOwned, manyToOneOwned1 );
					ManyToManyCompositeKey owning2 = new ManyToManyCompositeKey( oneToManyOwned, manyToOneOwned2 );

					entityManager.persist( oneToManyOwned );
					entityManager.persist( manyToOneOwned1 );
					entityManager.persist( manyToOneOwned2 );
					entityManager.persist( owning1 );
					entityManager.persist( owning2 );

					owning1Id = owning1.getId();
					owning2Id = owning2.getId();

					oneToManyId = oneToManyOwned.getId();
					manyToOne1Id = manyToOneOwned1.getId();
					manyToOne2Id = manyToOneOwned2.getId();
				}
		);

		// Revision 2
		inTransaction(
				entityManager -> {
					ManyToManyCompositeKey owning1 = entityManager.find( ManyToManyCompositeKey.class, owning1Id );
					entityManager.remove( owning1 );
				}
		);

		// Revision 3
		inTransaction(
				entityManager -> {
					ManyToManyCompositeKey owning2 = entityManager.find( ManyToManyCompositeKey.class, owning2Id );
					entityManager.remove( owning2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ManyToManyCompositeKey.class, owning1Id ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( ManyToManyCompositeKey.class, owning2Id ), contains( 1, 3 ) );

		assertThat( getAuditReader().getRevisions( OneToManyOwned.class, oneToManyId ), contains( 1 ) );

		assertThat( getAuditReader().getRevisions( ManyToOneOwned.class, manyToOne1Id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( ManyToOneOwned.class, manyToOne2Id ), contains( 1 ) );
	}

	@DynamicTest
	public void testOneToManyHistory() {
		final OneToManyOwned rev1 = getAuditReader().find( OneToManyOwned.class, oneToManyId, 1 );
		assertThat( rev1.getData(), equalTo( "data" ) );
		assertThat( rev1.getManyToManyCompositeKeys(), CollectionMatchers.hasSize( 2 ) );
	}

	@DynamicTest
	public void testManyToOne1History() {
		final ManyToOneOwned rev1 = getAuditReader().find( ManyToOneOwned.class, manyToOne1Id, 1 );
		assertThat( rev1.getData(), equalTo( "data1" ) );
	}

	@DynamicTest
	public void testManyToOne2History() {
		final ManyToOneOwned rev1 = getAuditReader().find( ManyToOneOwned.class, manyToOne2Id, 1 );
		assertThat( rev1.getData(), equalTo( "data2" ) );
	}

	@DynamicTest
	public void testOwning1History() {
		// objects
		final OneToManyOwned oneToMany = new OneToManyOwned( 1L, "data", null );
		final ManyToOneOwned manyToOne = new ManyToOneOwned( 2L, "data1" );

		// insert revision
		final ManyToManyCompositeKey rev1 = getAuditReader().find( ManyToManyCompositeKey.class, owning1Id, 1 );
		assertThat( rev1.getOneToMany(), equalTo( oneToMany ) );
		assertThat( rev1.getManyToOne(), equalTo( manyToOne ) );

		// removal revision - find returns null for deleted
		assertThat( getAuditReader().find( ManyToManyCompositeKey.class, owning1Id, 2 ), nullValue() );

		// fetch revision 2 using 'select deletions' api and verify.
		final ManyToManyCompositeKey rev2 = (ManyToManyCompositeKey) getAuditReader()
				.createQuery()
				.forRevisionsOfEntity( ManyToManyCompositeKey.class, true, true )
				.add( AuditEntity.id().eq( owning1Id ) )
				.add( AuditEntity.revisionNumber().eq( 2 ) )
				.getSingleResult();
		assertThat( rev2.getOneToMany(), equalTo( oneToMany ) );
		assertThat( rev2.getManyToOne(), equalTo( manyToOne ) );
	}

	@DynamicTest
	public void testOwning2History() {
		// objects
		final OneToManyOwned oneToMany = new OneToManyOwned( 1L, "data", null );
		final ManyToOneOwned manyToOne = new ManyToOneOwned( 3L, "data2" );

		// insert revision
		final ManyToManyCompositeKey rev1 = getAuditReader().find( ManyToManyCompositeKey.class, owning2Id, 1 );
		assertThat( rev1.getOneToMany(), equalTo( oneToMany ) );
		assertThat( rev1.getManyToOne(), equalTo( manyToOne ) );

		// removal revision - find returns null for deleted
		assertThat( getAuditReader().find( ManyToManyCompositeKey.class, owning2Id, 3 ), nullValue() );

		// fetch revision 3 using 'select deletions' api and verify.
		final ManyToManyCompositeKey rev2 = (ManyToManyCompositeKey) getAuditReader()
				.createQuery()
				.forRevisionsOfEntity( ManyToManyCompositeKey.class, true, true )
				.add( AuditEntity.id().eq( owning2Id ) )
				.add( AuditEntity.revisionNumber().eq( 3 ) )
				.getSingleResult();
		assertThat( rev2.getOneToMany(), equalTo( oneToMany ) );
		assertThat( rev2.getManyToOne(), equalTo( manyToOne ) );
	}
}
