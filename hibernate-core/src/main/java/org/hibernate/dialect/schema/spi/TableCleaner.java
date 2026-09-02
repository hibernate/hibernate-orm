/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.schema.spi;

import java.util.Collection;
import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Produces the complete ordered SQL used to clean mapped tables.
///
/// Report modes consistent with the command family returned by this cleaner.
/// Every result must be non-null, immutable, ordered, and contain no null
/// command.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getTableCleaner()
@Incubating
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface TableCleaner {
	ConstraintControlMode constraintControlMode();

	TruncateMode truncateMode();

	List<String> getSqlBeforeStrings();

	List<String> getSqlAfterStrings();

	List<String> getSqlDisableConstraintStrings(
			ForeignKey foreignKey,
			Metadata metadata,
			SqlStringGenerationContext context);

	List<String> getSqlEnableConstraintStrings(
			ForeignKey foreignKey,
			Metadata metadata,
			SqlStringGenerationContext context);

	List<String> getSqlTruncateStrings(
			Collection<Table> tables,
			Metadata metadata,
			SqlStringGenerationContext context);
}
