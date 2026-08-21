/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableSelectorStrategyTest {

	@Test
	public void testInitiallyEmpty() {
		TableSelectorStrategy strategy = new TableSelectorStrategy(new DefaultStrategy());
		List<RevengStrategy.SchemaSelection> selections = strategy.getSchemaSelections();
		assertNotNull(selections);
		assertTrue(selections.isEmpty());
	}

	@Test
	public void testAddSchemaSelection() {
		TableSelectorStrategy strategy = new TableSelectorStrategy(new DefaultStrategy());
		RevengStrategy.SchemaSelection selection = new RevengStrategy.SchemaSelection() {
			public String getMatchCatalog() { return "MY_CAT"; }
			public String getMatchSchema() { return "MY_SCH"; }
			public String getMatchTable() { return "MY_TABLE"; }
		};
		strategy.addSchemaSelection(selection);

		assertEquals(1, strategy.getSchemaSelections().size());
		assertEquals("MY_CAT", strategy.getSchemaSelections().get(0).getMatchCatalog());
	}

	@Test
	public void testClearSchemaSelections() {
		TableSelectorStrategy strategy = new TableSelectorStrategy(new DefaultStrategy());
		strategy.addSchemaSelection(new RevengStrategy.SchemaSelection() {
			public String getMatchCatalog() { return "C"; }
			public String getMatchSchema() { return "S"; }
			public String getMatchTable() { return "T"; }
		});
		assertEquals(1, strategy.getSchemaSelections().size());

		strategy.clearSchemaSelections();
		assertTrue(strategy.getSchemaSelections().isEmpty());
	}

	@Test
	public void testMultipleSelections() {
		TableSelectorStrategy strategy = new TableSelectorStrategy(new DefaultStrategy());
		for (int i = 0; i < 3; i++) {
			final int idx = i;
			strategy.addSchemaSelection(new RevengStrategy.SchemaSelection() {
				public String getMatchCatalog() { return "CAT_" + idx; }
				public String getMatchSchema() { return null; }
				public String getMatchTable() { return null; }
			});
		}
		assertEquals(3, strategy.getSchemaSelections().size());
	}
}
