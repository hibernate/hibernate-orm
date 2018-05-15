/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming.quotation;

import java.util.Arrays;
import java.util.Iterator;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.junit.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class QuotedFieldsTest extends BaseEnversJPAFunctionalTestCase {
	private Long qfeId1 = null;
	private Long qfeId2 = null;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {QuotedFieldsEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		QuotedFieldsEntity qfe1 = new QuotedFieldsEntity( "data1", 1 );
		QuotedFieldsEntity qfe2 = new QuotedFieldsEntity( "data2", 2 );

		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		em.persist( qfe1 );
		em.persist( qfe2 );
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		qfe1 = em.find( QuotedFieldsEntity.class, qfe1.getId() );
		qfe1.setData1( "data1 changed" );
		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();
		qfe2 = em.find( QuotedFieldsEntity.class, qfe2.getId() );
		qfe2.setData2( 3 );
		em.getTransaction().commit();

		qfeId1 = qfe1.getId();
		qfeId2 = qfe2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( QuotedFieldsEntity.class, qfeId1 ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( QuotedFieldsEntity.class, qfeId2 ) );
	}

	@Test
	public void testHistoryOfId1() {
		QuotedFieldsEntity ver1 = new QuotedFieldsEntity( qfeId1, "data1", 1 );
		QuotedFieldsEntity ver2 = new QuotedFieldsEntity( qfeId1, "data1 changed", 1 );

		assert getAuditReader().find( QuotedFieldsEntity.class, qfeId1, 1 ).equals( ver1 );
		assert getAuditReader().find( QuotedFieldsEntity.class, qfeId1, 2 ).equals( ver2 );
		assert getAuditReader().find( QuotedFieldsEntity.class, qfeId1, 3 ).equals( ver2 );
	}

	@Test
	public void testHistoryOfId2() {
		QuotedFieldsEntity ver1 = new QuotedFieldsEntity( qfeId2, "data2", 2 );
		QuotedFieldsEntity ver2 = new QuotedFieldsEntity( qfeId2, "data2", 3 );

		assert getAuditReader().find( QuotedFieldsEntity.class, qfeId2, 1 ).equals( ver1 );
		assert getAuditReader().find( QuotedFieldsEntity.class, qfeId2, 2 ).equals( ver1 );
		assert getAuditReader().find( QuotedFieldsEntity.class, qfeId2, 3 ).equals( ver2 );
	}

	@Test
	public void testEscapeEntityField() {
		Table table = metadata().getEntityBinding(
				"org.hibernate.envers.test.integration.naming.quotation.QuotedFieldsEntity_AUD"
		).getTable();
		Column column1 = getColumnByName( table, "id" );
		Column column2 = getColumnByName( table, "data1" );
		Column column3 = getColumnByName( table, "data2" );
		assert column1 != null;
		assert column2 != null;
		assert column3 != null;
		assert column1.isQuoted();
		assert column2.isQuoted();
		assert column3.isQuoted();
	}

	private Column getColumnByName(Table table, String columnName) {
		Iterator<Column> columnIterator = table.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			Column column = columnIterator.next();
			if ( columnName.equals( column.getName() ) ) {
				return column;
			}
		}
		return null;
	}
}
