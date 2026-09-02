/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.dialect.temptable.spi.TemporaryTableColumnDescriptor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UserDefinedObjectType;

/**
 * A pluggable contract that allows ordering of columns within {@link org.hibernate.mapping.Table},
 * {@link org.hibernate.mapping.Constraint} and {@link UserDefinedObjectType}.
 * <p>
 * An {@linkplain ColumnOrderingStrategy} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#COLUMN_ORDERING_STRATEGY}.
 *
 * @see org.hibernate.cfg.Configuration#setColumnOrderingStrategy(ColumnOrderingStrategy)
 * @see org.hibernate.boot.MetadataBuilder#applyColumnOrderingStrategy(ColumnOrderingStrategy)
 * @see org.hibernate.cfg.AvailableSettings#COLUMN_ORDERING_STRATEGY
 */
@Incubating
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface ColumnOrderingStrategy {

	/**
	 * Orders the columns of the table.
	 * May return null if columns were not ordered.
	 */
	List<Column> orderTableColumns(Table table, Metadata metadata);

	/**
	 * Orders the columns of the constraint.
	 * May return null if columns were not ordered.
	 */
	List<Column> orderConstraintColumns(Constraint constraint, Metadata metadata);

	/**
	 * Orders the columns of the user defined type.
	 * May return null if columns were not ordered.
	 */
	List<Column> orderUserDefinedTypeColumns(UserDefinedObjectType userDefinedType, Metadata metadata);

	/**
	 * Orders the columns of the temporary table.
	 */
	<C extends TemporaryTableColumnDescriptor> void orderTemporaryTableColumns(
			List<C> temporaryTableColumns,
			Metadata metadata);
}
