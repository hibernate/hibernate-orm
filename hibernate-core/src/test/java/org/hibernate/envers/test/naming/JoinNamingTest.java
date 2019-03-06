/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.naming;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.naming.JoinNamingRefEdEntity;
import org.hibernate.envers.test.support.domains.naming.JoinNamingRefIngEntity;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinNamingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ed_id1;
	private Integer ed_id2;
	private Integer ing_id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { JoinNamingRefEdEntity.class, JoinNamingRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					JoinNamingRefEdEntity ed1 = new JoinNamingRefEdEntity( "data1" );
					JoinNamingRefEdEntity ed2 = new JoinNamingRefEdEntity( "data2" );

					JoinNamingRefIngEntity ing1 = new JoinNamingRefIngEntity( "x", ed1 );

					entityManager.persist( ed1 );
					entityManager.persist( ed2 );
					entityManager.persist( ing1 );

					ed_id1 = ed1.getId();
					ed_id2 = ed2.getId();
					ing_id1 = ing1.getId();
				},

				// Revision 2
				entityManager -> {
					JoinNamingRefEdEntity ed2 = entityManager.find( JoinNamingRefEdEntity.class, ed_id2 );

					JoinNamingRefIngEntity ing1 = entityManager.find( JoinNamingRefIngEntity.class, ing_id1 );
					ing1.setData( "y" );
					ing1.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( JoinNamingRefEdEntity.class, ed_id1 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( JoinNamingRefEdEntity.class, ed_id2 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( JoinNamingRefIngEntity.class, ing_id1 ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id1, "data1" );

		assertThat( getAuditReader().find( JoinNamingRefEdEntity.class, ed_id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinNamingRefEdEntity.class, ed_id1, 2 ), equalTo( ver1 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		JoinNamingRefEdEntity ver1 = new JoinNamingRefEdEntity( ed_id2, "data2" );

		assertThat( getAuditReader().find( JoinNamingRefEdEntity.class, ed_id2, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinNamingRefEdEntity.class, ed_id2, 2 ), equalTo( ver1 ) );
	}

	@DynamicTest
	public void testHistoryOfIngId1() {
		JoinNamingRefIngEntity ver1 = new JoinNamingRefIngEntity( ing_id1, "x", null );
		JoinNamingRefIngEntity ver2 = new JoinNamingRefIngEntity( ing_id1, "y", null );

		assertThat( getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 2 ), equalTo( ver2 ) );

		final JoinNamingRefEdEntity ed_ver1 = new JoinNamingRefEdEntity( ed_id1, "data1" );
		assertThat( getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 1 ).getReference(), equalTo( ed_ver1 ) );

		final JoinNamingRefEdEntity ed_ver2 = new JoinNamingRefEdEntity( ed_id2, "data2" );
		assertThat( getAuditReader().find( JoinNamingRefIngEntity.class, ing_id1, 2 ).getReference(), equalTo( ed_ver2 ) );
	}

	@SuppressWarnings({"unchecked"})
	@DynamicTest
	public void testJoinColumnName() {
		final EntityTypeDescriptor descriptor = getMetamodel().getEntityDescriptor( JoinNamingRefIngEntity.class.getName() + "_AUD" );
		List<Column> columns = descriptor.findPersistentAttribute( "reference_id" ).getColumns();
		assertThat( columns, CollectionMatchers.isNotEmpty() );
		assertThat( columns.stream().map( Column::getExpression ).collect( Collectors.toList() ), contains( "jnree_column_reference" ) );
	}
}