/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query;

import java.util.List;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntTestEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@SuppressWarnings({"unchecked"})
@Disabled("NYI - Limit support not yet implemented")
public class OrderByLimitQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;
	private Integer id4;
	private Integer id5;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { IntTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final IntTestEntity ite1 = new IntTestEntity( 12 );
					final IntTestEntity ite2 = new IntTestEntity( 5 );
					final IntTestEntity ite3 = new IntTestEntity( 8 );
					final IntTestEntity ite4 = new IntTestEntity( 1 );

					entityManager.persist( ite1 );
					entityManager.persist( ite2 );
					entityManager.persist( ite3 );
					entityManager.persist( ite4 );

					id1 = ite1.getId();
					id2 = ite2.getId();
					id3 = ite3.getId();
					id4 = ite4.getId();
				},

				// Revision 2
				entityManager -> {
					final IntTestEntity ite5 = new IntTestEntity( 3 );
					entityManager.persist( ite5 );
					id5 = ite5.getId();

					final IntTestEntity ite1 = entityManager.find( IntTestEntity.class, id1 );
					ite1.setNumber( 0 );

					final IntTestEntity ite4 = entityManager.find( IntTestEntity.class, id4 );
					ite4.setNumber( 15 );
				}
		);
	}

	@DynamicTest
	public void testEntitiesOrderLimitByQueryRev1() {
		List<IntTestEntity> res_0_to_1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 0 )
				.setMaxResults( 2 )
				.getResultList();

		List<IntTestEntity> res_2_to_3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 2 )
				.setMaxResults( 2 )
				.getResultList();

		List<IntTestEntity> res_empty = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 1 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 4 )
				.setMaxResults( 2 )
				.getResultList();

		assertThat( res_0_to_1, contains( new IntTestEntity( id1, 12 ), new IntTestEntity( id3, 8 ) ) );
		assertThat( res_2_to_3, contains( new IntTestEntity( id2, 5 ), new IntTestEntity( id4, 1 ) ) );
		assertThat( res_empty, CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testEntitiesOrderLimitByQueryRev2() {
		List<IntTestEntity> res_0_to_1 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 0 )
				.setMaxResults( 2 )
				.getResultList();

		List<IntTestEntity> res_2_to_3 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 2 )
				.setMaxResults( 2 )
				.getResultList();

		List<IntTestEntity> res_4 = getAuditReader().createQuery()
				.forEntitiesAtRevision( IntTestEntity.class, 2 )
				.addOrder( AuditEntity.property( "number" ).desc() )
				.setFirstResult( 4 )
				.setMaxResults( 2 )
				.getResultList();

		assertThat( res_0_to_1, contains( new IntTestEntity( id4, 15 ), new IntTestEntity( id3, 8 ) ) );
		assertThat( res_2_to_3, contains( new IntTestEntity( id2, 5 ), new IntTestEntity( id5, 3 ) ) );
		assertThat( res_4, contains( new IntTestEntity( id1, 0 ) ) );
	}
}