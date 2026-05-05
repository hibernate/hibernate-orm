/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.meta;

import org.hibernate.sql.model.TableMapping;


/// Details about a table in the domain model, used to creation mutation
/// operations in the graph-based ActionQueue implementation.
///
/// @author Steve Ebersole
public interface TableDescriptor {
	/// The table's name.
	String name();

	/// Whether the table is considered optional in relation to its group of tables.
	///
	/// @implNote Effectively means a [jakarta.persistence.SecondaryTable] mapped as
	/// [org.hibernate.annotations.SecondaryRow#optional()].
	///
	/// @see #getRelativePosition()
	boolean isOptional();

	/// Details about the key for this table.
	///
	/// @todo Currently for collections this is the foreign-key, but imo this should always be the primary key
	///  	and collection table descriptor should just have separate foreignKeyDescriptor attribute
	TableKeyDescriptor keyDescriptor();

	/// Whether this table has foreign-keys which refer back to the same table.
	///
	/// @implNote E.g., an `employee` table which has a `manager_fk` key that targets back at `employee`.`
	boolean isSelfReferential();

	boolean hasUniqueConstraints();

	/// Whether cascade deletion is defined on the underlying database table.
	boolean cascadeDeleteEnabled();

	/// Details about insertions into this table.
	TableMapping.MutationDetails insertDetails();

	/// Details about updates to this table.
	TableMapping.MutationDetails updateDetails();

	/// Details about deletions to this table.
	TableMapping.MutationDetails deleteDetails();

	/// This table's relative position within its "table group".
	int getRelativePosition();
}
