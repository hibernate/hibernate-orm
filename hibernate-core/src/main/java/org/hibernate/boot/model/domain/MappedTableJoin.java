/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import org.hibernate.boot.model.relational.MappedTable;

/**
 * Common representation of both SecondaryTable and joined-tables
 * from inheritance.
 *
 * @author Steve Ebersole
 */
public interface MappedTableJoin {
	MappedTable getMappedTable();
	boolean isOptional();
	// todo (6.0) : access of some sort to column mappings to be able to generate "join predicate" in persisters
}
