package org.hibernate.orm.test.function.xml;

import java.util.List;

import org.hibernate.dialect.function.xml.XmlTableFunction;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmXmlTableFunction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Copying an {@code xmltable()} function must leave the original as it was
 * and give a copy with the same arguments and columns.
 */
@DomainModel
@SessionFactory
@JiraKey("HHH-21160")
public class XmlTableCopyTest {

	@Test
	public void copyWithOneColumn(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final SqmXmlTableFunction<?> table = xmlTable( (NodeBuilder) session.getCriteriaBuilder() );
			table.ordinalityColumn( "idx" );
			assertCopy( table );
		} );
	}

	@Test
	public void copyWithSeveralColumns(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final SqmXmlTableFunction<?> table = xmlTable( (NodeBuilder) session.getCriteriaBuilder() );
			table.valueColumn( "val", String.class );
			table.queryColumn( "node" );
			table.ordinalityColumn( "idx" );
			assertCopy( table );
		} );
	}

	private static void assertCopy(SqmXmlTableFunction<?> table) {
		final String rendered = table.toHqlString();
		final SqmXmlTableFunction<?> copy = table.copy( SqmCopyContext.simpleContext() );
		assertNotSame( table, copy );
		assertEquals( rendered, table.toHqlString() );
		assertEquals( rendered, copy.toHqlString() );
		assertTrue( table.isCompatible( copy ) );
		assertEquals( table.cacheHashCode(), copy.cacheHashCode() );
		copy.valueColumn( "extra", String.class );
		assertFalse( table.isCompatible( copy ) );
	}

	private static SqmXmlTableFunction<?> xmlTable(NodeBuilder cb) {
		// xmltable() is not registered for every dialect, but copying it does not depend on the dialect
		final XmlTableFunction descriptor = new XmlTableFunction( true, cb.getTypeConfiguration() );
		return (SqmXmlTableFunction<?>) descriptor.generateSqmExpression(
				List.of( cb.literal( "/root/elem" ), cb.literal( "<root><elem>1</elem></root>" ) ),
				cb.getQueryEngine()
		);
	}
}
