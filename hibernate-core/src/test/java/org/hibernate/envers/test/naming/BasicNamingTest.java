/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.naming;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.naming.NamingTestEntity1;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicNamingTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { NamingTestEntity1.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					NamingTestEntity1 nte1 = new NamingTestEntity1( "data1" );
					NamingTestEntity1 nte2 = new NamingTestEntity1( "data2" );

					entityManager.persist( nte1 );
					entityManager.persist( nte2 );

					id1 = nte1.getId();
					id2 = nte2.getId();
				},

				// Revision 2
				entityManager -> {
					NamingTestEntity1 nte1 = entityManager.find( NamingTestEntity1.class, id1 );
					nte1.setData( "data1'" );
				},

				// Revision 3
				entityManager -> {
					NamingTestEntity1 nte2 = entityManager.find( NamingTestEntity1.class, id2 );
					nte2.setData( "data2'" );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( NamingTestEntity1.class, id1 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( NamingTestEntity1.class, id2 ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		NamingTestEntity1 ver1 = new NamingTestEntity1( id1, "data1" );
		NamingTestEntity1 ver2 = new NamingTestEntity1( id1, "data1'" );

		assertThat( getAuditReader().find( NamingTestEntity1.class, id1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( NamingTestEntity1.class, id1, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( NamingTestEntity1.class, id1, 3 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId2() {
		NamingTestEntity1 ver1 = new NamingTestEntity1( id2, "data2" );
		NamingTestEntity1 ver2 = new NamingTestEntity1( id2, "data2'" );

		assertThat( getAuditReader().find( NamingTestEntity1.class, id2, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( NamingTestEntity1.class, id2, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( NamingTestEntity1.class, id2, 3 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testTableName() {
		final EntityTypeDescriptor descriptor = getAuditEntityDescriptor( NamingTestEntity1.class );
		assertThat( descriptor, notNullValue() );
		assertThat( descriptor.getPrimaryTable().getTableExpression(), is( "naming_test_entity_1_versions" ) );
	}
}
