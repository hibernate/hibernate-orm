/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.sources;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OrderColumn;

/// Unified source for column-like mapping annotations.
///
/// Several JPA annotations describe a physical column but use slightly different
/// defaults and applicability rules.  `ColumnSource` gives column binders one
/// view over those annotations while preserving the source role: attribute
/// column, join column, map key column, order column, or discriminator column.
///
/// @since 9.0
/// @author Steve Ebersole
public interface ColumnSource {
	static ColumnSource from(jakarta.persistence.Column column) {
		return column == null ? null : new JpaColumnSource( column );
	}

	static ColumnSource from(JoinColumn joinColumn) {
		return joinColumn == null ? null : new JoinColumnSource( joinColumn );
	}

	static ColumnSource from(MapKeyColumn mapKeyColumn) {
		return mapKeyColumn == null ? null : new MapKeyColumnSource( mapKeyColumn );
	}

	static ColumnSource from(MapKeyJoinColumn mapKeyJoinColumn) {
		return mapKeyJoinColumn == null ? null : new MapKeyJoinColumnSource( mapKeyJoinColumn );
	}

	static ColumnSource from(OrderColumn orderColumn) {
		return orderColumn == null ? null : new OrderColumnSource( orderColumn );
	}

	static ColumnSource from(DiscriminatorColumn discriminatorColumn) {
		return discriminatorColumn == null ? null : new DiscriminatorColumnSource( discriminatorColumn );
	}

	String name();

	default String nonEmptyName() {
		return StringHelper.nullIfEmpty( name() );
	}

	boolean unique(boolean defaultValue);

	boolean nullable(boolean defaultValue);

	String columnDefinition();

	int length(int defaultValue);

	int precision(int defaultValue);

	int scale(int defaultValue);

	String options();

	String table();

	default CheckConstraint[] checkConstraints() {
		return new CheckConstraint[0];
	}

	record JpaColumnSource(jakarta.persistence.Column column) implements ColumnSource {
		@Override
		public String name() {
			return column.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return column.unique();
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return column.nullable();
		}

		@Override
		public String columnDefinition() {
			return column.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return column.length();
		}

		@Override
		public int precision(int defaultValue) {
			return column.precision();
		}

		@Override
		public int scale(int defaultValue) {
			return column.scale();
		}

		@Override
		public String options() {
			return column.options();
		}

		@Override
		public String table() {
			return column.table();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return column.check();
		}
	}

	record JoinColumnSource(JoinColumn joinColumn) implements ColumnSource {
		@Override
		public String name() {
			return joinColumn.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return joinColumn.unique();
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return joinColumn.nullable();
		}

		@Override
		public String columnDefinition() {
			return joinColumn.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int precision(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int scale(int defaultValue) {
			return defaultValue;
		}

		@Override
		public String options() {
			return joinColumn.options();
		}

		@Override
		public String table() {
			return joinColumn.table();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return joinColumn.check();
		}
	}

	record MapKeyColumnSource(MapKeyColumn mapKeyColumn) implements ColumnSource {
		@Override
		public String name() {
			return mapKeyColumn.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return mapKeyColumn.unique();
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return mapKeyColumn.nullable();
		}

		@Override
		public String columnDefinition() {
			return mapKeyColumn.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return mapKeyColumn.length();
		}

		@Override
		public int precision(int defaultValue) {
			return mapKeyColumn.precision();
		}

		@Override
		public int scale(int defaultValue) {
			return mapKeyColumn.scale();
		}

		@Override
		public String options() {
			return mapKeyColumn.options();
		}

		@Override
		public String table() {
			return mapKeyColumn.table();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return mapKeyColumn.check();
		}
	}

	record MapKeyJoinColumnSource(MapKeyJoinColumn mapKeyJoinColumn) implements ColumnSource {
		@Override
		public String name() {
			return mapKeyJoinColumn.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return mapKeyJoinColumn.unique();
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return mapKeyJoinColumn.nullable();
		}

		@Override
		public String columnDefinition() {
			return mapKeyJoinColumn.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int precision(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int scale(int defaultValue) {
			return defaultValue;
		}

		@Override
		public String options() {
			return "";
		}

		@Override
		public String table() {
			return mapKeyJoinColumn.table();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return mapKeyJoinColumn.check();
		}
	}

	record OrderColumnSource(OrderColumn orderColumn) implements ColumnSource {
		@Override
		public String name() {
			return orderColumn.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return defaultValue;
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return orderColumn.nullable();
		}

		@Override
		public String columnDefinition() {
			return orderColumn.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int precision(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int scale(int defaultValue) {
			return defaultValue;
		}

		@Override
		public String options() {
			return orderColumn.options();
		}

		@Override
		public String table() {
			return "";
		}
	}

	record DiscriminatorColumnSource(DiscriminatorColumn discriminatorColumn) implements ColumnSource {
		@Override
		public String name() {
			return discriminatorColumn.name();
		}

		@Override
		public boolean unique(boolean defaultValue) {
			return defaultValue;
		}

		@Override
		public boolean nullable(boolean defaultValue) {
			return defaultValue;
		}

		@Override
		public String columnDefinition() {
			return discriminatorColumn.columnDefinition();
		}

		@Override
		public int length(int defaultValue) {
			return discriminatorColumn.length();
		}

		@Override
		public int precision(int defaultValue) {
			return defaultValue;
		}

		@Override
		public int scale(int defaultValue) {
			return defaultValue;
		}

		@Override
		public String options() {
			return discriminatorColumn.options();
		}

		@Override
		public String table() {
			return "";
		}
	}
}
