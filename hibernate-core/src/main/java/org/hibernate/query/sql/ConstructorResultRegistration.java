/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql;

import java.util.List;

import org.hibernate.query.ConstructorEntityArgumentMode;

/**
 * Describes a "constructor ("dynamic instantiation") query result.
 *
 * Defined via any of:
 *
 * 	    * {@link javax.persistence.ConstructorResult}
 * 	    * `<constructor-result/>` in `orm.xml`
 *
 * @author Steve Ebersole
 */
public interface ConstructorResultRegistration<T> extends ReturnableResultRegistration {
	/**
	 * The Java type to be instantiated
	 */
	Class<T> getTargetClass();

	/**
	 * The mode describing how to manage entity references as a constructor
	 * argument.
	 *
	 * todo (6.0) - allow this per result ref?  or stop at query-level?
	 */
	ConstructorEntityArgumentMode getEntityArgumentMode();

	/**
	 * The "query results" making up the constructor arguments.
	 *
	 * NOTE that JPA only allows these to be scalar values
	 * (ColumnResult/ScalarResultRegistration) while Hibernate
	 * allows more extended options
	 */
	List<ReturnableResultRegistration> getArguments();
}
