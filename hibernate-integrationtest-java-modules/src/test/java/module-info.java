module org.hibernate.orm.integrationtest.java.module.test {

	/*
	 * Main configuration, necessary for real client applications.
	 */

	opens org.hibernate.orm.integrationtest.java.module.test.entity to
			org.hibernate.orm.core;

	requires jakarta.persistence;
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
