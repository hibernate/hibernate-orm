/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.cfg.RevengDialectFactoryTest;

import org.hibernate.dialect.*;
import org.hibernate.tool.reveng.api.reveng.RevengDialectFactory;
import org.hibernate.tool.reveng.internal.dialect.*;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class TestCase {

	private static class NoNameDialect extends Dialect {
		public NoNameDialect() {
			super((DatabaseVersion)null);
		}
	}

	private static class H2NamedDialect extends Dialect {
		public H2NamedDialect() {
			super((DatabaseVersion)null);
		}
	}

	@Test
	public void testCreateRevengDialect() {
		assertSameClass(
				"Generic metadata for dialects with no specifics",
				JDBCRevengDialect.class,
				RevengDialectFactory.createMetaDataDialect(
						new NoNameDialect(),
						new Properties()));
		assertSameClass(
				H2RevengDialect.class,
				RevengDialectFactory.createMetaDataDialect(new H2NamedDialect(), new Properties()));
		assertSameClass(
				OracleRevengDialect.class,
				RevengDialectFactory.createMetaDataDialect(
						new OracleDialect(),
						new Properties()));
		assertSameClass(
				MySQLRevengDialect.class,
				RevengDialectFactory.createMetaDataDialect(
						new MySQLDialect(),
						new Properties()));
		Properties p = new Properties();
		p.setProperty(
				"hibernatetool.metadatadialect",
				H2RevengDialect.class.getCanonicalName());
		assertSameClass(
				"property should override specific dialect",
				H2RevengDialect.class,
				RevengDialectFactory.createMetaDataDialect(new MySQLDialect(), p));
	}

	@Test
	public void testCreateRevengDialectNonExistingOverride() {
		Properties p = new Properties();
		p.setProperty("hibernatetool.metadatadialect", "DoesNotExists");
		try {
			RevengDialectFactory.createMetaDataDialect(new MySQLDialect(), p);
			fail();
		}
		catch (RuntimeException jbe) {
			// expected
		} catch(Exception e) {
			fail();
		}
	}

	@Test
	public void testFromDialect() {
		assertSameClass(
				"Generic metadata for dialects with no specifics",
				null,
				RevengDialectFactory.fromDialect(new NoNameDialect()));
		assertSameClass(
				OracleRevengDialect.class,
				RevengDialectFactory.fromDialect(new OracleDialect()));
		assertSameClass(
				MySQLRevengDialect.class,
				RevengDialectFactory.fromDialect(new MySQLDialect()));
		assertSameClass(
				H2RevengDialect.class,
				RevengDialectFactory.fromDialect(new H2Dialect()));
		assertSameClass(
				HSQLRevengDialect.class,
				RevengDialectFactory.fromDialect(new HSQLDialect()));

	}

	@Test
	public void testFromDialectName() {
		assertSameClass(
				null,
				RevengDialectFactory.fromDialectName("BlahBlah"));
		assertSameClass(
				OracleRevengDialect.class,
				RevengDialectFactory.fromDialectName("mYorAcleDialect"));
		assertSameClass(
				OracleRevengDialect.class,
				RevengDialectFactory.fromDialectName(OracleDialect.class.getName()));
		assertSameClass(
				MySQLRevengDialect.class,
				RevengDialectFactory.fromDialectName(MySQLDialect.class.getName()));
		assertSameClass(
				H2RevengDialect.class,
				RevengDialectFactory.fromDialectName(H2Dialect.class.getName()));
		assertSameClass(
				HSQLRevengDialect.class,
				RevengDialectFactory.fromDialectName(HSQLDialect.class.getName()));

	}

	private void assertSameClass(Class<?> clazz, Object instance) {
		if(clazz==null) {
			assertNull(instance);
			return;
		}
		if(instance==null) {
			assertNull(clazz.getCanonicalName());
			return;
		}
		assertEquals(clazz.getCanonicalName(), instance.getClass().getName());
	}

	private void assertSameClass(String msg, Class<?> clazz, Object instance) {
		if(clazz==null) {
			assertNull(instance, msg);
			return;
		}
		if(instance==null) {
			assertNull(clazz.getCanonicalName(), msg);
			return;
		}
		assertEquals(clazz.getCanonicalName(), instance.getClass().getName(), msg);
	}
}
