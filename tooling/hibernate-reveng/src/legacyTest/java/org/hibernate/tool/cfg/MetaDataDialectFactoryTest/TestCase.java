/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hibernate.tool.cfg.MetaDataDialectFactoryTest;

import org.hibernate.dialect.*;
import org.hibernate.tool.api.reveng.RevengDialectFactory;
import org.hibernate.tool.internal.reveng.dialect.*;
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
	public void testCreateMetaDataDialect() {
		assertSameClass(
				"Generic metadata for dialects with no specifics", 
				JDBCMetaDataDialect.class, 
				RevengDialectFactory.createMetaDataDialect(
						new NoNameDialect(), 
						new Properties()));
		assertSameClass(
				H2MetaDataDialect.class, 
				RevengDialectFactory.createMetaDataDialect(new H2NamedDialect(), new Properties()));
		assertSameClass(
				OracleMetaDataDialect.class, 
				RevengDialectFactory.createMetaDataDialect(
						new OracleDialect(), 
						new Properties()));		
		assertSameClass(
				MySQLMetaDataDialect.class, 
				RevengDialectFactory.createMetaDataDialect(
						new MySQLDialect(), 
						new Properties()));
		Properties p = new Properties();
		p.setProperty(
				"hibernatetool.metadatadialect", 
				H2MetaDataDialect.class.getCanonicalName());
		assertSameClass(
				"property should override specific dialect", 
				H2MetaDataDialect.class, 
				RevengDialectFactory.createMetaDataDialect(new MySQLDialect(), p));
	}

	@Test
	public void testCreateMetaDataDialectNonExistingOverride() {
		Properties p = new Properties();
		p.setProperty("hibernatetool.metadatadialect", "DoesNotExists");
		try {
			RevengDialectFactory.createMetaDataDialect(new MySQLDialect(), p);
			fail();
		} catch (RuntimeException jbe) {
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
				OracleMetaDataDialect.class, 
				RevengDialectFactory.fromDialect(new OracleDialect()));
		assertSameClass(
				MySQLMetaDataDialect.class, 
				RevengDialectFactory.fromDialect(new MySQLDialect()));
		assertSameClass(
				H2MetaDataDialect.class, 
				RevengDialectFactory.fromDialect(new H2Dialect()));
		assertSameClass(
				HSQLMetaDataDialect.class,
				RevengDialectFactory.fromDialect(new HSQLDialect()));
		
	}

	@Test
	public void testFromDialectName() {
		assertSameClass(
				null, 
				RevengDialectFactory.fromDialectName("BlahBlah"));
		assertSameClass(
				OracleMetaDataDialect.class, 
				RevengDialectFactory.fromDialectName("mYorAcleDialect"));
		assertSameClass(
				OracleMetaDataDialect.class, 
				RevengDialectFactory.fromDialectName(OracleDialect.class.getName()));
		assertSameClass(
				MySQLMetaDataDialect.class, 
				RevengDialectFactory.fromDialectName(MySQLDialect.class.getName()));
		assertSameClass(
				H2MetaDataDialect.class, 
				RevengDialectFactory.fromDialectName(H2Dialect.class.getName()));
		assertSameClass(
				HSQLMetaDataDialect.class, 
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
