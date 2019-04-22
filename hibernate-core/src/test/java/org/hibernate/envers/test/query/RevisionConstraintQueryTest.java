/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;

import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrIntTestEntity;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
public class RevisionConstraintQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrIntTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final StrIntTestEntity site1 = new StrIntTestEntity( "a", 10 );
					final StrIntTestEntity site2 = new StrIntTestEntity( "b", 15 );

					entityManager.persist( site1 );
					entityManager.persist( site2 );

					id1 = site1.getId();
					id2 = site2.getId();
				},

				// Revision 2
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );

					site1.setStr1( "d" );
					site2.setNumber( 20 );
				},

				// Revision 3
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );

					site1.setNumber( 1 );
					site2.setStr1( "z" );
				},

				// Revision 4
				entityManager -> {
					final StrIntTestEntity site1 = entityManager.find( StrIntTestEntity.class, id1 );
					final StrIntTestEntity site2 = entityManager.find( StrIntTestEntity.class, id2 );

					site1.setNumber( 5 );
					site2.setStr1( "a" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsLtQuery() {
		List<Number> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().distinct() )
				.add( AuditEntity.revisionNumber().lt( 3 ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		assertThat( result, contains( 1, 2 ) );
	}

	@DynamicTest
	public void testRevisionsGeQuery() {
		List<Number> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().distinct() )
				.add( AuditEntity.revisionNumber().ge( 2 ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		assertThat( result, contains( 2, 3, 4 ) );
	}

	@DynamicTest
	public void testRevisionsLeWithPropertyQuery() {
		List<Number> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.revisionNumber().le( 3 ) )
				.add( AuditEntity.property( "str1" ).eq( "a" ) )
				.getResultList();

		assertThat( result, contains( 1 ) );
	}

	@DynamicTest
	public void testRevisionsGtWithPropertyQuery() {
		List<Number> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.revisionNumber().gt( 1 ) )
				.add( AuditEntity.property( "number" ).lt( 10 ) )
				.addOrder( AuditEntity.revisionNumber().asc() )
				.getResultList();

		assertThat( result, contains( 3, 4 ) );
	}

	@DynamicTest
	public void testRevisionProjectionQuery() {
		Object[] result = (Object[]) getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().max() )
				.addProjection( AuditEntity.revisionNumber().count() )
				.addProjection( AuditEntity.revisionNumber().countDistinct() )
				.addProjection( AuditEntity.revisionNumber().min() )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		assertThat( result[0], equalTo( 4 ) );
		assertThat( result[1], equalTo( 4L ) );
		assertThat( result[2], equalTo( 4L ) );
		assertThat( result[3], equalTo( 1 ) );
	}

	@DynamicTest
	public void testRevisionOrderQuery() {
		List<Number> result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber() )
				.add( AuditEntity.id().eq( id1 ) )
				.addOrder( AuditEntity.revisionNumber().desc() )
				.getResultList();

		assertThat( result, contains( 4, 3, 2, 1 ) );
	}

	@DynamicTest
	public void testRevisionCountQuery() {
		// The query shouldn't be ordered as always, otherwise - we get an exception.
		Object result = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, false, true )
				.addProjection( AuditEntity.revisionNumber().count() )
				.add( AuditEntity.id().eq( id1 ) )
				.getSingleResult();

		assertThat( result, equalTo( 4L ) );
	}

	@DynamicTest
	public void testRevisionTypeEqQuery() {
		// The query shouldn't be ordered as always, otherwise - we get an exception.
		List<StrIntTestEntity> results = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.id().eq( id1 ) )
				.add( AuditEntity.revisionType().eq( RevisionType.MOD ) )
				.getResultList();

		assertThat( results, CollectionMatchers.hasSize( 3 ) );
		assertThat( results.get( 0 ), equalTo( new StrIntTestEntity( "d", 10, id1 ) ) );
		assertThat( results.get( 1 ), equalTo( new StrIntTestEntity( "d", 1, id1 ) ) );
		assertThat( results.get( 2 ), equalTo( new StrIntTestEntity( "d", 5, id1 ) ) );
	}

	@DynamicTest
	public void testRevisionTypeNeQuery() {
		// The query shouldn't be ordered as always, otherwise - we get an exception.
		List<StrIntTestEntity> results = getAuditReader().createQuery()
				.forRevisionsOfEntity( StrIntTestEntity.class, true, true )
				.add( AuditEntity.id().eq( id1 ) )
				.add( AuditEntity.revisionType().ne( RevisionType.MOD ) )
				.getResultList();

		assertThat( results, CollectionMatchers.hasSize( 1 ) );
		assertThat( results.get( 0 ), equalTo( new StrIntTestEntity( "a", 10, id1 ) ) );
	}
}