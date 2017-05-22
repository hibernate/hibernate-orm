/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;

/**
 * @author Steve Ebersole
 */
public interface TableGroupContext {
	QuerySpec getQuerySpec();
	TableSpace getTableSpace();

	/**
	 * The unique identifier (uid) to use for the table group being generated
	 */
	String getUniqueIdentifier();

	/**
	 * Access to the "identification variable" (alias) as defined in the source.
	 * Should never be {@code null}; if a particular source reference defines no
	 * identification variable the implementor should use a generated one.
	 */
	String getIdentificationVariable();

	/**
	 * The specific entity subclass to be used (for filtering).
	 */
	EntityTypeImplementor getIntrinsicSubclassEntityMetadata();

	TableGroupResolver getTableGroupResolver();

	SqlAliasBaseGenerator getSqlAliasBaseGenerator();

	/**
	 * The join type to be used for joins between TableReferences within a TableGroup
	 */
	JoinType getTableReferenceJoinType();
}
