/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.naming.ids;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.naming.ids.JoinMulIdNamingRefEdEntity;
import org.hibernate.envers.test.support.domains.naming.ids.JoinMulIdNamingRefIngEntity;
import org.hibernate.envers.test.support.domains.naming.ids.MulIdNaming;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - @IdClass / Multiple @Id support")
public class JoinMultipleIdNamingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private static final MulIdNaming REF_ED1_ID = new MulIdNaming( 10, 20 );
	private static final MulIdNaming REF_ED2_ID = new MulIdNaming( 11, 21 );
	private static final MulIdNaming REF_ING1_ID = new MulIdNaming( 12, 22 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { JoinMulIdNamingRefEdEntity.class, JoinMulIdNamingRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					JoinMulIdNamingRefEdEntity ed1 = new JoinMulIdNamingRefEdEntity( REF_ED1_ID, "data1" );
					JoinMulIdNamingRefEdEntity ed2 = new JoinMulIdNamingRefEdEntity( REF_ED2_ID, "data2" );
					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					JoinMulIdNamingRefIngEntity ing1 = new JoinMulIdNamingRefIngEntity( REF_ING1_ID, "x", ed1 );
					entityManager.persist( ing1 );
				},

				// Revision 2
				entityManager -> {
					JoinMulIdNamingRefEdEntity ed2 = entityManager.find( JoinMulIdNamingRefEdEntity.class, REF_ED2_ID );
					JoinMulIdNamingRefIngEntity ing1 = entityManager.find( JoinMulIdNamingRefIngEntity.class, REF_ING1_ID );
					ing1.setData( "y" );
					ing1.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( JoinMulIdNamingRefEdEntity.class, REF_ED1_ID ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( JoinMulIdNamingRefEdEntity.class, REF_ED2_ID ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( JoinMulIdNamingRefIngEntity.class, REF_ING1_ID ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		final JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity( REF_ED1_ID, "data1" );
		assertThat( getAuditReader().find( JoinMulIdNamingRefEdEntity.class, REF_ED1_ID, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinMulIdNamingRefEdEntity.class, REF_ED1_ID, 2 ), equalTo( ver1 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		final JoinMulIdNamingRefEdEntity ver1 = new JoinMulIdNamingRefEdEntity( REF_ED2_ID, "data2" );
		assertThat( getAuditReader().find( JoinMulIdNamingRefEdEntity.class, REF_ED2_ID, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinMulIdNamingRefEdEntity.class, REF_ED2_ID, 2 ), equalTo( ver1 ) );
	}

	@DynamicTest
	public void testHistoryOfIngId1() {
		JoinMulIdNamingRefIngEntity ver1 = new JoinMulIdNamingRefIngEntity( REF_ING1_ID, "x", null );
		JoinMulIdNamingRefIngEntity ver2 = new JoinMulIdNamingRefIngEntity( REF_ING1_ID, "y", null );

		assertThat( getAuditReader().find( JoinMulIdNamingRefIngEntity.class, REF_ING1_ID, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinMulIdNamingRefIngEntity.class, REF_ING1_ID, 2 ), equalTo( ver2 ) );

		final JoinMulIdNamingRefEdEntity ed1 = new JoinMulIdNamingRefEdEntity( REF_ED1_ID, "data1" );
		final JoinMulIdNamingRefEdEntity ed2 = new JoinMulIdNamingRefEdEntity( REF_ED2_ID, "data2" );

		assertThat( getAuditReader().find( JoinMulIdNamingRefIngEntity.class, REF_ING1_ID, 1 ).getReference(), equalTo( ed1 ) );
		assertThat( getAuditReader().find( JoinMulIdNamingRefIngEntity.class, REF_ING1_ID, 2 ).getReference(), equalTo( ed2 ) );
	}

	@DynamicTest
	public void testJoinColumnNames() {
		final List<Column> id1Columns = getAttributeColumns( JoinMulIdNamingRefIngEntity.class, "reference_id1" );
		assertThat( id1Columns.stream().map( Column::getExpression ).collect( Collectors.toList() ), contains( "ID1_reference" ) );

		final List<Column> id2Columns = getAttributeColumns( JoinMulIdNamingRefIngEntity.class, "reference_id2" );
		assertThat( id2Columns.stream().map( Column::getExpression ).collect( Collectors.toList() ), contains( "ID2_reference" ) );
	}

	@SuppressWarnings({"unchecked"})
	private List<Column> getAttributeColumns(Class<?> clazz, String attributeName) {
		return ( (NonIdPersistentAttribute) getAuditEntityDescriptor( clazz ).getAttribute( attributeName ) ).getColumns();
	}
}