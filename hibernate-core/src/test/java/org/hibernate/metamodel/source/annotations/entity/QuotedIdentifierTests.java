package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.relational.Identifier;

import static org.junit.Assert.assertEquals;

/**
 * @author Strong Liu
 */
public class QuotedIdentifierTests extends BaseAnnotationBindingTestCase {
	private final String ormPath = "org/hibernate/metamodel/source/annotations/xml/orm-quote-identifier.xml";

	@Test
	@Resources(annotatedClasses = { Item.class, Item2.class, Item3.class, Item4.class }, ormXmlPath = ormPath)
	public void testDelimitedIdentifiers() {
		EntityBinding item = getEntityBinding( Item.class );
		assertIdentifierEquals( "`Item`", item );

		item = getEntityBinding( Item2.class );
		assertIdentifierEquals( "`TABLE_ITEM2`", item );

		item = getEntityBinding( Item3.class );
		assertIdentifierEquals( "`TABLE_ITEM3`", item );

		item = getEntityBinding( Item4.class );
		assertIdentifierEquals( "`TABLE_ITEM4`", item );
	}

	private void assertIdentifierEquals(String expected, EntityBinding realValue) {
		org.hibernate.metamodel.relational.Table table = (org.hibernate.metamodel.relational.Table) realValue.getBaseTable();
		assertEquals( Identifier.toIdentifier( expected ), table.getTableName() );
	}

	@Entity
	private static class Item {
		@Id
		Long id;
	}

	@Entity
	@Table(name = "TABLE_ITEM2")
	private static class Item2 {
		@Id
		Long id;
	}

	@Entity
	@Table(name = "`TABLE_ITEM3`")
	private static class Item3 {
		@Id
		Long id;
	}

	@Entity
	@Table(name = "\"TABLE_ITEM4\"")
	private static class Item4 {
		@Id
		Long id;
	}
}
