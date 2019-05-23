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
import org.hibernate.envers.test.support.domains.naming.ids.EmbIdNaming;
import org.hibernate.envers.test.support.domains.naming.ids.JoinEmbIdNamingRefEdEntity;
import org.hibernate.envers.test.support.domains.naming.ids.JoinEmbIdNamingRefIngEntity;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class JoinEmbeddedIdNamingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private static final EmbIdNaming REF_ED1_ID = new EmbIdNaming( 10, 20 );
	private static final EmbIdNaming REF_ED2_ID = new EmbIdNaming( 11, 21 );
	private static final EmbIdNaming REF_ING1_ID = new EmbIdNaming( 12, 22 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { JoinEmbIdNamingRefEdEntity.class, JoinEmbIdNamingRefIngEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					JoinEmbIdNamingRefEdEntity ed1 = new JoinEmbIdNamingRefEdEntity( REF_ED1_ID, "data1" );
					JoinEmbIdNamingRefEdEntity ed2 = new JoinEmbIdNamingRefEdEntity( REF_ED2_ID, "data2" );
					entityManager.persist( ed1 );
					entityManager.persist( ed2 );

					JoinEmbIdNamingRefIngEntity ing1 = new JoinEmbIdNamingRefIngEntity( REF_ING1_ID, "x", ed1 );
					entityManager.persist( ing1 );
				},

				// Revision 2
				entityManager -> {
					JoinEmbIdNamingRefEdEntity ed2 = entityManager.find( JoinEmbIdNamingRefEdEntity.class, REF_ED2_ID );
					JoinEmbIdNamingRefIngEntity ing1 = entityManager.find( JoinEmbIdNamingRefIngEntity.class, REF_ING1_ID );
					ing1.setData( "y" );
					ing1.setReference( ed2 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( JoinEmbIdNamingRefEdEntity.class, REF_ED1_ID ), contains( 1, 2 ) );
		assertThat(	getAuditReader().getRevisions( JoinEmbIdNamingRefEdEntity.class, REF_ED2_ID ), contains( 1, 2 ) );
		assertThat(	getAuditReader().getRevisions( JoinEmbIdNamingRefIngEntity.class, REF_ING1_ID ), contains( 1, 2 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId1() {
		final JoinEmbIdNamingRefEdEntity ver1 = new JoinEmbIdNamingRefEdEntity( REF_ED1_ID, "data1" );
		assertThat( getAuditReader().find( JoinEmbIdNamingRefEdEntity.class, REF_ED1_ID, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinEmbIdNamingRefEdEntity.class, REF_ED1_ID, 2 ), equalTo( ver1 ) );
	}

	@DynamicTest
	public void testHistoryOfEdId2() {
		final JoinEmbIdNamingRefEdEntity ver1 = new JoinEmbIdNamingRefEdEntity( REF_ED2_ID, "data2" );
		assertThat( getAuditReader().find( JoinEmbIdNamingRefEdEntity.class, REF_ED2_ID, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinEmbIdNamingRefEdEntity.class, REF_ED2_ID, 2 ), equalTo( ver1 ) );
	}

	@DynamicTest
	public void testHistoryOfIngId1() {
		final JoinEmbIdNamingRefIngEntity ver1 = new JoinEmbIdNamingRefIngEntity( REF_ING1_ID, "x", null );
		final JoinEmbIdNamingRefIngEntity ver2 = new JoinEmbIdNamingRefIngEntity( REF_ING1_ID, "y", null );

		assertThat( getAuditReader().find( JoinEmbIdNamingRefIngEntity.class, REF_ING1_ID, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( JoinEmbIdNamingRefIngEntity.class, REF_ING1_ID, 2 ), equalTo( ver2 ) );

		final JoinEmbIdNamingRefEdEntity ed1 = new JoinEmbIdNamingRefEdEntity( REF_ED1_ID, "data1" );
		final JoinEmbIdNamingRefEdEntity ed2 = new JoinEmbIdNamingRefEdEntity( REF_ED2_ID, "data2" );

		assertThat( getAuditReader().find( JoinEmbIdNamingRefIngEntity.class, REF_ING1_ID, 1 ).getReference(), equalTo( ed1 ) );
		assertThat( getAuditReader().find( JoinEmbIdNamingRefIngEntity.class, REF_ING1_ID, 2 ).getReference(), equalTo( ed2 ) );
	}

	@DynamicTest
	public void testJoinColumnNames() {
		final List<Column> xColumns = getAttributeColumns( JoinEmbIdNamingRefIngEntity.class, "reference_x" );
		assertThat( xColumns.stream().map( Column::getExpression ).collect( Collectors.toList() ), contains( "XX_reference" ) );

		final List<Column> yColumns = getAttributeColumns( JoinEmbIdNamingRefIngEntity.class, "reference_y" );
		assertThat( yColumns.stream().map( Column::getExpression ).collect( Collectors.toList() ), contains( "YY_reference" ) );
	}

	@SuppressWarnings({"unchecked"})
	private List<Column> getAttributeColumns(Class<?> clazz, String attributeName) {
		return ( (NonIdPersistentAttribute) getAuditEntityDescriptor( clazz ).getAttribute( attributeName ) ).getColumns();
	}
}
