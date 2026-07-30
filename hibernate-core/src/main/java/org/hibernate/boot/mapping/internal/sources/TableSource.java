/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Index;
import jakarta.persistence.JoinTable;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.UniqueConstraint;

/// Unified source for physical table-like mapping annotations.
///
/// Entity tables, secondary tables, join tables, and collection tables all
/// provide table name, catalog, schema, comment, and options.  `TableSource`
/// keeps those common facts available to [org.hibernate.boot.mapping.internal.binders.TableBinder]
/// while allowing the caller to retain the source role separately.
///
/// @since 9.0
/// @author Steve Ebersole
public interface TableSource {
	static TableSource from(jakarta.persistence.Table table) {
		return table == null ? null : new JpaTableSource( table );
	}

	static TableSource from(JoinTable joinTable) {
		return joinTable == null ? null : new JoinTableSource( joinTable );
	}

	static TableSource from(CollectionTable collectionTable) {
		return collectionTable == null ? null : new CollectionTableSource( collectionTable );
	}

	static TableSource from(SecondaryTable secondaryTable) {
		return secondaryTable == null ? null : new SecondaryTableSource( secondaryTable );
	}

	String name();

	default String nonEmptyName() {
		return StringHelper.nullIfEmpty( name() );
	}

	String schema();

	String catalog();

	String comment();

	String options();

	String type();

	CheckConstraint[] checkConstraints();

	UniqueConstraint[] uniqueConstraints();

	Index[] indexes();

	record JpaTableSource(jakarta.persistence.Table table) implements TableSource {
		@Override
		public String name() {
			return table.name();
		}

		@Override
		public String schema() {
			return table.schema();
		}

		@Override
		public String catalog() {
			return table.catalog();
		}

		@Override
		public String comment() {
			return table.comment();
		}

		@Override
		public String options() {
			return table.options();
		}

		@Override
		public String type() {
			return table.type();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return table.check();
		}

		@Override
		public UniqueConstraint[] uniqueConstraints() {
			return table.uniqueConstraints();
		}

		@Override
		public Index[] indexes() {
			return table.indexes();
		}
	}

	record JoinTableSource(JoinTable joinTable) implements TableSource {
		@Override
		public String name() {
			return joinTable.name();
		}

		@Override
		public String schema() {
			return joinTable.schema();
		}

		@Override
		public String catalog() {
			return joinTable.catalog();
		}

		@Override
		public String comment() {
			return joinTable.comment();
		}

		@Override
		public String options() {
			return joinTable.options();
		}

		@Override
		public String type() {
			return joinTable.type();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return joinTable.check();
		}

		@Override
		public UniqueConstraint[] uniqueConstraints() {
			return joinTable.uniqueConstraints();
		}

		@Override
		public Index[] indexes() {
			return joinTable.indexes();
		}
	}

	record SecondaryTableSource(SecondaryTable secondaryTable) implements TableSource {
		@Override
		public String name() {
			return secondaryTable.name();
		}

		@Override
		public String schema() {
			return secondaryTable.schema();
		}

		@Override
		public String catalog() {
			return secondaryTable.catalog();
		}

		@Override
		public String comment() {
			return secondaryTable.comment();
		}

		@Override
		public String options() {
			return secondaryTable.options();
		}

		@Override
		public String type() {
			return secondaryTable.type();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return secondaryTable.check();
		}

		@Override
		public UniqueConstraint[] uniqueConstraints() {
			return secondaryTable.uniqueConstraints();
		}

		@Override
		public Index[] indexes() {
			return secondaryTable.indexes();
		}
	}

	record CollectionTableSource(CollectionTable collectionTable) implements TableSource {
		@Override
		public String name() {
			return collectionTable.name();
		}

		@Override
		public String schema() {
			return collectionTable.schema();
		}

		@Override
		public String catalog() {
			return collectionTable.catalog();
		}

		@Override
		public String comment() {
			return collectionTable.comment();
		}

		@Override
		public String options() {
			return collectionTable.options();
		}

		@Override
		public String type() {
			return collectionTable.type();
		}

		@Override
		public CheckConstraint[] checkConstraints() {
			return collectionTable.check();
		}

		@Override
		public UniqueConstraint[] uniqueConstraints() {
			return collectionTable.uniqueConstraints();
		}

		@Override
		public Index[] indexes() {
			return collectionTable.indexes();
		}
	}
}
