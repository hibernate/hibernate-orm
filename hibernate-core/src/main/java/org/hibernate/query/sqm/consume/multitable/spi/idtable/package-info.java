/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for {@link org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy}
 * implementations which are backed by an "id table" which is used to temporarily store
 * the ids of entities matching the update/delete restrictions.
 *
 * General execution processing via both
 * {@link org.hibernate.query.sqm.consume.multitable.spi.UpdateHandler} and
 * {@link org.hibernate.query.sqm.consume.multitable.spi.DeleteHandler}
 * follows the same steps:<ol>
 *     <li>
 *         (possibly) create the id-table
 *     </li>
 *     <li>
 *         perform the INSERT-SELECT to populate the id-table with ids of
 *         entities matching the restriction specified on the SQM
 *         update or delete query being handled
 *     </li>
 *     <li>
 *         for each entity table perform the physical update or delete as
 *         indicated by the SQM delete or update query
 *     </li>
 *     <li>
 *         (possibly) delete the id-table rows - should probably just always
 *         do that first (unless we just created the table in step 1)
 *     </li>
 *     <li>
 *         (possibly) drop the id-table
 *     </li>
 * </ol>
 *
 * todo (6.0) : do 1 and 5 really ever happen today?
 * 		I guess the id-table has to be created there if the table
 * 		is a local temp table - maybe in that case it also needs to
 * 		be manually dropped
 * 		+
 * 		Any other cases?
 * 		+
 * 		Note that generally speaking the id-table is created and dropped
 * 		at the Strategy level during
 * 		{@link org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy#prepare}
 * 		and {@link org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy#release}
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;
