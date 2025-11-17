/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.UnknownParameterException;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * Mutation for a specific table as part of a logical mutation on the entity.
 *
 * Functional behavior is driven by the specific subtypes: <ul>
 *     <li>{@link PreparableMutationOperation}</li>
 *     <li>{@link SelfExecutingUpdateOperation}</li>
 * </ul>
 *
 *
 * example #1 - delete simple entity (Person)
 * 		(1) MutationOperation(DELETE, person)
 *
 * example #2 - delete entity (Person) w/ secondary *non-optional* table
 * 		(1) MutationOperation(DELETE, person)
 * 		(2) MutationOperation(DELETE, person_supp)
 *
 * example #3 - insert simple entity (Person)
 * 		(1) MutationOperation(INSERT, person)
 *
 * example #4 - insert entity (Person) w/ secondary *non-optional* table &amp; IDENTITY id column
 * 		(1) MutationOperation(INSERT, person)
 * 		(2) MutationOperation(INSERT, person_supp)
 *
 * example #6 - update/merge entity (Person) w/ secondary *optional* table
 * 		(1) MutationOperation(UPDATE, person)
 * 		(2) MutationOperation(UPDATE, person_supp) - upsert / delete
 *
 *
 * account for batching
 *
 * example #1 - insert entity (Person) w/ secondary *optional* table
 * 		(1) MutationOperation(INSERT, person) - batched, all
 * 		(2) MutationOperation(INSERT, person_supp) - batched, conditional[1]
 *
 * example #2 - delete entity (Person) w/ secondary table
 * 		(1) MutationOperation(DELETE, person) - batched, all
 * 		(2) MutationOperation(DELETE, person_supp) - batched, all
 *
 * example #3 - update/merge entity (Person) w/ secondary *optional* table
 * 		(1) MutationOperation(UPDATE, person) - batched
 * 		(2) MutationOperation(UPDATE, person_supp) - non-batched
 *
 *
 * [1] For insertions with optional secondary tables, if the values are all null for that table we
 * do not want to perform the "add-to-batch" handling for that specific "row"
 *
 * @author Steve Ebersole
 */
@Incubating
public interface MutationOperation {
	/**
	 * The type of operation (INSERT, etc)
	 */
	MutationType getMutationType();

	/**
	 * The thing being mutated
	 */
	MutationTarget<?> getMutationTarget();

	/**
	 * The table against which operation is to be performed
	 */
	TableMapping getTableDetails();

	/**
	 * Find the JDBC parameter to be used for the specified column.
	 *
	 * @return The descriptor, or null if none match.
	 *
	 * @see #getJdbcValueDescriptor
	 */
	JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage);

	/**
	 * Form of {@link #findValueDescriptor}, throwing an exception if not found as opposed
	 * to simply returning null
	 */
	default JdbcValueDescriptor getJdbcValueDescriptor(String columnName, ParameterUsage usage) {
		final JdbcValueDescriptor parameterDescriptor = findValueDescriptor( columnName, usage );
		if ( parameterDescriptor == null ) {
			throw new UnknownParameterException( getMutationType(), getMutationTarget(), getTableDetails().getTableName(), columnName, usage );
		}
		return parameterDescriptor;
	}
}
