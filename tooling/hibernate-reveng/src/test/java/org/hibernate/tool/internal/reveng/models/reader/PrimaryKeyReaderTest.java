/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.persistence.GenerationType;

import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.models.reader.ModelsDatabaseSchemaReaderTest.TestRevengDialect;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PrimaryKeyReader}.
 *
 * @author Koen Aers
 */
public class PrimaryKeyReaderTest {

	private DefaultStrategy strategy;
	private TestRevengDialect dialect;
	private TableIdentifier tableId;

	@BeforeEach
	public void setUp() {
		strategy = new DefaultStrategy();
		RevengSettings settings = new RevengSettings(strategy);
		settings.setDefaultPackageName("com.example");
		strategy.setSettings(settings);
		dialect = new TestRevengDialect();
		tableId = TableIdentifier.create(null, null, "TEST_TABLE");
	}

	@Test
	public void testSinglePrimaryKey() {
		dialect.addPrimaryKey("TEST_TABLE", "ID", 1);

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, strategy);
		Set<String> result = reader.readPrimaryKeys(null, null, "TEST_TABLE", tableId);

		assertEquals(1, result.size());
		assertTrue(result.contains("ID"));
	}

	@Test
	public void testCompositePrimaryKey() {
		dialect.addPrimaryKey("TEST_TABLE", "ORDER_ID", 1);
		dialect.addPrimaryKey("TEST_TABLE", "PRODUCT_ID", 2);

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, strategy);
		Set<String> result = reader.readPrimaryKeys(null, null, "TEST_TABLE", tableId);

		assertEquals(2, result.size());
		assertTrue(result.contains("ORDER_ID"));
		assertTrue(result.contains("PRODUCT_ID"));
	}

	@Test
	public void testNoPrimaryKeys() {
		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, strategy);
		Set<String> result = reader.readPrimaryKeys(null, null, "TEST_TABLE", tableId);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testFallbackToStrategy() {
		DefaultStrategy fallbackStrategy = new DefaultStrategy() {
			@Override
			public List<String> getPrimaryKeyColumnNames(TableIdentifier identifier) {
				return Arrays.asList("COL_A", "COL_B");
			}
		};
		RevengSettings settings = new RevengSettings(fallbackStrategy);
		settings.setDefaultPackageName("com.example");
		fallbackStrategy.setSettings(settings);

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, fallbackStrategy);
		Set<String> result = reader.readPrimaryKeys(null, null, "TEST_TABLE", tableId);

		assertEquals(2, result.size());
		assertTrue(result.contains("COL_A"));
		assertTrue(result.contains("COL_B"));
	}

	@Test
	public void testDialectTakesPrecedenceOverStrategy() {
		dialect.addPrimaryKey("TEST_TABLE", "ID", 1);

		DefaultStrategy fallbackStrategy = new DefaultStrategy() {
			@Override
			public List<String> getPrimaryKeyColumnNames(TableIdentifier identifier) {
				return Arrays.asList("OTHER_COL");
			}
		};
		RevengSettings settings = new RevengSettings(fallbackStrategy);
		settings.setDefaultPackageName("com.example");
		fallbackStrategy.setSettings(settings);

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, fallbackStrategy);
		Set<String> result = reader.readPrimaryKeys(null, null, "TEST_TABLE", tableId);

		assertEquals(1, result.size());
		assertTrue(result.contains("ID"));
		assertFalse(result.contains("OTHER_COL"));
	}

	@Test
	public void testStrategyReturnsNull() {
		DefaultStrategy nullStrategy = new DefaultStrategy() {
			@Override
			public List<String> getPrimaryKeyColumnNames(TableIdentifier identifier) {
				return null;
			}
		};
		RevengSettings settings = new RevengSettings(nullStrategy);
		settings.setDefaultPackageName("com.example");
		nullStrategy.setSettings(settings);

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, nullStrategy);
		Set<String> result = reader.readPrimaryKeys(null, null, "TEST_TABLE", tableId);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testReadIdentifierStrategyFromDialect() {
		dialect.addSuggestedPrimaryKeyStrategy("TEST_TABLE", "identity");

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, strategy);
		String result = reader.readIdentifierStrategy(null, null, "TEST_TABLE", tableId);

		assertEquals("identity", result);
	}

	@Test
	public void testReadIdentifierStrategyFromStrategy() {
		DefaultStrategy customStrategy = new DefaultStrategy() {
			@Override
			public String getTableIdentifierStrategyName(TableIdentifier identifier) {
				return "sequence";
			}
		};
		RevengSettings settings = new RevengSettings(customStrategy);
		settings.setDefaultPackageName("com.example");
		customStrategy.setSettings(settings);

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, customStrategy);
		String result = reader.readIdentifierStrategy(null, null, "TEST_TABLE", tableId);

		assertEquals("sequence", result);
	}

	@Test
	public void testReadIdentifierStrategyReturnsNullWhenNone() {
		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, strategy);
		String result = reader.readIdentifierStrategy(null, null, "TEST_TABLE", tableId);

		assertNull(result);
	}

	@Test
	public void testStrategyTakesPrecedenceOverDialect() {
		dialect.addSuggestedPrimaryKeyStrategy("TEST_TABLE", "identity");
		DefaultStrategy customStrategy = new DefaultStrategy() {
			@Override
			public String getTableIdentifierStrategyName(TableIdentifier identifier) {
				return "sequence";
			}
		};
		RevengSettings settings = new RevengSettings(customStrategy);
		settings.setDefaultPackageName("com.example");
		customStrategy.setSettings(settings);

		PrimaryKeyReader reader = PrimaryKeyReader.create(dialect, customStrategy);
		String result = reader.readIdentifierStrategy(null, null, "TEST_TABLE", tableId);

		assertEquals("sequence", result);
	}

	@Test
	public void testToGenerationTypeIdentity() {
		assertEquals(GenerationType.IDENTITY, PrimaryKeyReader.toGenerationType("identity"));
	}

	@Test
	public void testToGenerationTypeSequence() {
		assertEquals(GenerationType.SEQUENCE, PrimaryKeyReader.toGenerationType("sequence"));
	}

	@Test
	public void testToGenerationTypeAssigned() {
		assertNull(PrimaryKeyReader.toGenerationType("assigned"));
	}

	@Test
	public void testToGenerationTypeNull() {
		assertNull(PrimaryKeyReader.toGenerationType(null));
	}

	@Test
	public void testToGenerationTypeOther() {
		assertEquals(GenerationType.AUTO, PrimaryKeyReader.toGenerationType("native"));
	}
}
