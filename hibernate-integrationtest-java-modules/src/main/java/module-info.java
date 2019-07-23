/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
module org.hibernate.orm.integrationtest.java.module {
	exports org.hibernate.orm.integrationtest.java.module.service;
	opens org.hibernate.orm.integrationtest.java.module.entity to
			org.hibernate.orm.core,
			javassist; // Necessary for javassist, but not for bytebuddy (the default)

	requires java.persistence;
	/*
	 * IDEA will not find the modules below because it apparently doesn't support automatic module names
	 * for modules in the current project.
	 * Everything should work fine when building from the command line, though.
	 */
	requires org.hibernate.orm.core;
	requires org.hibernate.orm.envers;

	/*
	 * This is necessary in order to use SessionFactory,
	 * which extends "javax.naming.Referenceable".
	 * Without this, compilation as a Java module fails.
	 */
	requires java.naming;
}