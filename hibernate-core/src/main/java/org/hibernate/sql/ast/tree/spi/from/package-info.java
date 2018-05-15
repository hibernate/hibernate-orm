/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Models the concepts in the SQL {@code <from clause>}.
 * <p/>
 * The lowest level of this modeling is the {@link org.hibernate.sql.ast.tree.spi.from.TableReference}
 * contract which models a table as known to the Hibernate persisters.  This might be a physical table
 * or a derived table (Hibernate subselect mapping).
 * <p/>
 * TableSpecifications are grouped into a {@link org.hibernate.sql.ast.tree.spi.from.TableGroup} representing
 * all of the TableSpecifications originating from a particular persister reference.  Think of this piece as the
 * bridge between persister references in a query and the tables represented by that persister reference.
 * <p/>
 * Finally TableSpecificationGroups are themselves grouped into a {@link org.hibernate.sql.ast.tree.spi.from.TableSpace}.
 *
 * todo : example of TableSpace e.g. {@code ... from Entity1, Entity2}
 */
package org.hibernate.sql.ast.tree.spi.from;
