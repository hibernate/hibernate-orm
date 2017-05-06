/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.List;

/**
 * Common contract for an entity's non-root tables.  This includes:<ul>
 *     <li>secondary tables</li>
 *     <li>joined inheritance tables</li>
 * </ul>
 * @author Steve Ebersole
 */
public interface NonRootTableBinding {
	/**
	 * Obtain the non-root Table that is bound as the joined table (the
	 * "right hand side").
	 */
	Table getTargetTable();

	/**
	 * Is the join optional?  In other words, should we OUTER join this
	 * table, or is INNER joining a possibility?  {@code true} indicates
	 * that we must use an OUTER join. {@code false} (required) indicates
	 * that we can use an INNER join, depending on context.
	 */
	boolean isOptional();

	/**
	 * Obtain the columns from the "target table" ({@link #getTargetTable()} that
	 * make up the join predicate.  We expect that they columns match to the root
	 * table's PK columns.
	 * <p/>
	 * <b>NOTE : order is expected to match the root table's PK columns.</b>
	 */
	List<Column> getJoinColumns();
}
