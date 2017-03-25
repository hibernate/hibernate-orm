/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SchemaUpdateProceedOnErrorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			From.class
		};
	}

	@Override
	protected Map buildSettings() {
		Map settings = super.buildSettings();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "update" );
		return settings;
	}

	@Override
	public void buildEntityManagerFactory() throws Exception {
		try {
			super.buildEntityManagerFactory();
		}
		catch ( Exception e ) {
			fail("Should not halt on error: " + e.getMessage());
		}
	}

	@Test
	public void testHaltOnError() {
	}

	@Entity(name = "From")
	public class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
