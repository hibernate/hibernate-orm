/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.tool.schemacreation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.orm.junit.RequiresDialect;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(value = AbstractHANADialect.class, matchSubTypes = true)
public class AbstractHANAStringAndBooleanFieldsCreationTest extends BaseSchemaCreationTestCase {
	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {

		@Id
		private String field;

		private char c;

		@Lob
		private String lob;

		private boolean b;
	}
}
