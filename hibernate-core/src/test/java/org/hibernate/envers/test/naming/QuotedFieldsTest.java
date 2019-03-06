/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.naming;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.naming.QuotedFieldsEntity;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.Table;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class QuotedFieldsTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Long qfeId1 = null;
	private Long qfeId2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { QuotedFieldsEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					QuotedFieldsEntity qfe1 = new QuotedFieldsEntity( "data1", 1 );
					QuotedFieldsEntity qfe2 = new QuotedFieldsEntity( "data2", 2 );

					entityManager.persist( qfe1 );
					entityManager.persist( qfe2 );

					qfeId1 = qfe1.getId();
					qfeId2 = qfe2.getId();
				},

				// Revision 2
				entityManager -> {
					QuotedFieldsEntity qfe1 = entityManager.find( QuotedFieldsEntity.class, qfeId1 );
					qfe1.setData1( "data1 changed" );
				},

				// Revision 3
				entityManager -> {
					QuotedFieldsEntity qfe2 = entityManager.find( QuotedFieldsEntity.class, qfeId2 );
					qfe2.setData2( 3 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( QuotedFieldsEntity.class, qfeId1 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( QuotedFieldsEntity.class, qfeId2 ), contains( 1, 3 ) );
	}

	@DynamicTest
	public void testHistoryOfId1() {
		QuotedFieldsEntity ver1 = new QuotedFieldsEntity( qfeId1, "data1", 1 );
		QuotedFieldsEntity ver2 = new QuotedFieldsEntity( qfeId1, "data1 changed", 1 );

		assertThat( getAuditReader().find( QuotedFieldsEntity.class, qfeId1, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( QuotedFieldsEntity.class, qfeId1, 2 ), equalTo( ver2 ) );
		assertThat( getAuditReader().find( QuotedFieldsEntity.class, qfeId1, 3 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testHistoryOfId2() {
		QuotedFieldsEntity ver1 = new QuotedFieldsEntity( qfeId2, "data2", 2 );
		QuotedFieldsEntity ver2 = new QuotedFieldsEntity( qfeId2, "data2", 3 );

		assertThat( getAuditReader().find( QuotedFieldsEntity.class, qfeId2, 1 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( QuotedFieldsEntity.class, qfeId2, 2 ), equalTo( ver1 ) );
		assertThat( getAuditReader().find( QuotedFieldsEntity.class, qfeId2, 3 ), equalTo( ver2 ) );
	}

	@DynamicTest
	public void testEscapeEntityField() {
		final Table primaryTable = getMetamodel().findEntityDescriptor( QuotedFieldsEntity.class.getName() + "_AUD" ).getPrimaryTable();
		assertColumnIsQuoted( primaryTable, "id" );
		assertColumnIsQuoted( primaryTable, "data1" );
		assertColumnIsQuoted( primaryTable, "data2" );
	}

	private static void assertColumnIsQuoted(Table table, String columnName) {
		final Column column = table.getColumn( columnName );
		assertThat( column, notNullValue() );
		assertThat( column, instanceOf( PhysicalColumn.class ) );

		final PhysicalColumn physicalColumn = (PhysicalColumn) column;
		assertThat( physicalColumn.getName().isQuoted(), is( true ) );
	}
}
