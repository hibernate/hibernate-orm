/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DelegatingStrategyTest {

	@Test
	public void testNullDelegateReturnsDefaults() {
		DelegatingStrategy strategy = new DelegatingStrategy(null);
		TableIdentifier ti = TableIdentifier.create("cat", "sch", "PERSON");

		assertNull(strategy.columnToPropertyName(ti, "name"));
		assertFalse(strategy.excludeTable(ti));
		assertFalse(strategy.excludeColumn(ti, "name"));
		assertNull(strategy.tableToClassName(ti));
		assertNull(strategy.getTableIdentifierStrategyName(ti));
		assertNull(strategy.getTableIdentifierProperties(ti));
		assertNull(strategy.getPrimaryKeyColumnNames(ti));
		assertNull(strategy.classNameToCompositeIdName("Person"));
		assertNull(strategy.getOptimisticLockColumnName(ti));
		assertFalse(strategy.useColumnForOptimisticLock(ti, "version"));
		assertNull(strategy.getSchemaSelections());
		assertNull(strategy.tableToIdentifierPropertyName(ti));
		assertNull(strategy.tableToCompositeIdName(ti));
		assertNull(strategy.getForeignKeys(ti));
		assertNull(strategy.tableToMetaAttributes(ti));
		assertNull(strategy.columnToMetaAttributes(ti, "col"));
		assertNull(strategy.foreignKeyToAssociationInfo(null));
		assertNull(strategy.foreignKeyToInverseAssociationInfo(null));
	}

	@Test
	public void testNullDelegateBooleanDefaults() {
		DelegatingStrategy strategy = new DelegatingStrategy(null);
		TableIdentifier ti = TableIdentifier.create("cat", "sch", "PERSON");

		assertFalse(strategy.excludeForeignKeyAsCollection("fk", ti, Collections.emptyList(), ti, Collections.emptyList()));
		assertFalse(strategy.excludeForeignKeyAsManytoOne("fk", ti, Collections.emptyList(), ti, Collections.emptyList()));
		assertTrue(strategy.isForeignKeyCollectionLazy("fk", ti, Collections.emptyList(), ti, Collections.emptyList()));
		assertTrue(strategy.isManyToManyTable(null));
		assertTrue(strategy.isOneToOne(null));
	}

	@Test
	public void testNullDelegateCollectionName() {
		DelegatingStrategy strategy = new DelegatingStrategy(null);
		TableIdentifier ti = TableIdentifier.create("cat", "sch", "PERSON");
		assertNull(strategy.foreignKeyToCollectionName("fk", ti, Collections.emptyList(), ti, Collections.emptyList(), true));
		assertNull(strategy.foreignKeyToEntityName("fk", ti, Collections.emptyList(), ti, Collections.emptyList(), true));
		assertNull(strategy.foreignKeyToInverseEntityName("fk", ti, Collections.emptyList(), ti, Collections.emptyList(), true));
	}

	@Test
	public void testNullDelegateColumnToHibernateType() {
		DelegatingStrategy strategy = new DelegatingStrategy(null);
		TableIdentifier ti = TableIdentifier.create("cat", "sch", "PERSON");
		assertNull(strategy.columnToHibernateTypeName(ti, "col", 12, 255, 0, 0, true, false));
	}

	@Test
	public void testNullDelegateForeignKeyToManyToManyName() {
		DelegatingStrategy strategy = new DelegatingStrategy(null);
		assertNull(strategy.foreignKeyToManyToManyName(null, null, null, true));
	}

	@Test
	public void testNullDelegateClose() {
		DelegatingStrategy strategy = new DelegatingStrategy(null);
		assertDoesNotThrow(strategy::close);
	}

	@Test
	public void testNullDelegateSetSettings() {
		DelegatingStrategy strategy = new DelegatingStrategy(null);
		assertDoesNotThrow(() -> strategy.setSettings(null));
	}

	@Test
	public void testDelegatesWithRealStrategy() {
		DefaultStrategy real = new DefaultStrategy();
		DelegatingStrategy strategy = new DelegatingStrategy(real);
		TableIdentifier ti = TableIdentifier.create(null, null, "PERSON");

		assertEquals("Person", strategy.tableToClassName(ti));
		assertEquals("PersonId", strategy.classNameToCompositeIdName("Person"));
	}
}
