/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * A set of rules for determining the physical names of objects in a relational
 * database schema from the logical names specified by the object/relational
 * mappings.
 * <ul>
 *     <li>A <em>physical name</em> is a name used to interact with the database,
 *     and will always be used in generated SQL, both DML and DDL.
 *     <li>A <em>logical name</em> is a name used to within annotations of Java
 *     code and XML mapping documents.
 * </ul>
 * <p>
 * Logical names provide an additional level of indirection between the mappings
 * and the database schema, and a {@code PhysicalNamingStrategy} even allows the
 * use of more "natural" naming within the mappings in cases where the relational
 * schema features especially inelegant legacy naming conventions. For example,
 * it could shield the mappings from old-fashioned practices like prefixing table
 * names with {@code TBL_}.
 * <p>
 * Note, however, that handwritten native SQL must be written in terms of physical
 * names, so the abstraction here is in some sense "incomplete".
 * <p>
 * A {@code PhysicalNamingStrategy} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY}.
 *
 * @see ImplicitNamingStrategy
 * @see org.hibernate.cfg.Configuration#setPhysicalNamingStrategy(PhysicalNamingStrategy)
 * @see org.hibernate.boot.MetadataBuilder#applyPhysicalNamingStrategy(PhysicalNamingStrategy)
 * @see org.hibernate.cfg.AvailableSettings#PHYSICAL_NAMING_STRATEGY
 *
 * @author Steve Ebersole
 */
@Incubating
public interface PhysicalNamingStrategy {
	/**
	 * Determine the physical catalog name from the given logical name
	 */
	Identifier toPhysicalCatalogName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the physical schema name from the given logical name
	 */
	Identifier toPhysicalSchemaName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the physical table name from the given logical name
	 */
	Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the physical sequence name from the given logical name
	 */
	Identifier toPhysicalSequenceName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the physical column name from the given logical name
	 */
	Identifier toPhysicalColumnName(Identifier logicalName, JdbcEnvironment jdbcEnvironment);

	/**
	 * Determine the physical UDT type name from the given logical name
	 */
	default Identifier toPhysicalTypeName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
		return toPhysicalTableName( logicalName, jdbcEnvironment );
	}
}
