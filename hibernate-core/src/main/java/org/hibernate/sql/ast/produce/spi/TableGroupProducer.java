/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

/**
 * Marker interface for anything which produces a TableGroup
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface TableGroupProducer {
	/**
	 * Get the "stem" used as the base for generating SQL table aliases for table
	 * references that are part of the TableGroup being generated
	 * <p/>
	 * Note that this is a metadata-ive value.  It is only ever used internal to
	 * the producer producing its TableGroup.
	 *
	 * @see org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager#createSqlAliasBase
	 */
	String getSqlAliasStem();
}
