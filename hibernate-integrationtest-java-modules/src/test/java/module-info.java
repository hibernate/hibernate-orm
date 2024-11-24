/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
module org.hibernate.orm.integrationtest.java.module.test {

	/*
	 * Main configuration, necessary for real client applications.
	 */

	opens org.hibernate.orm.integrationtest.java.module.test.entity to org.hibernate.orm.core;

	requires java.persistence;
	// IDEA will not find the modules below because it apparently doesn't support automatic module names
	// for modules in the current project.
	// Everything should work fine when building from the command line, though.
	requires org.hibernate.orm.core;
	requires org.hibernate.orm.envers;

	// Transitive dependencies that leak through the Hibernate ORM API
	requires java.sql;
	requires java.naming; // SessionFactory extends "javax.naming.Referenceable"

	/*
	 * Test-only configuration.
	 */

	opens org.hibernate.orm.integrationtest.java.module.test to junit;
	requires junit;
}

