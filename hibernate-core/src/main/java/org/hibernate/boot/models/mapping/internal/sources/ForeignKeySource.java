/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.sources;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.SecondaryTable;

/// Unified source for foreign-key mapping metadata.
///
/// Foreign-key metadata can be declared on join columns, join tables, collection
/// tables, and secondary tables.  This source descriptor lets association,
/// table-key, and foreign-key phases carry the same customization information
/// without each phase re-reading the original annotation type.
///
/// @since 9.0
/// @author Steve Ebersole
public interface ForeignKeySource {
	static ForeignKeySource from(ForeignKey foreignKey) {
		return foreignKey == null ? null : new JpaForeignKeySource( foreignKey );
	}

	static ForeignKeySource from(JoinColumn joinColumn) {
		return joinColumn == null ? null : from( joinColumn.foreignKey() );
	}

	static ForeignKeySource from(JoinTable joinTable) {
		return joinTable == null ? null : from( joinTable.foreignKey() );
	}

	static ForeignKeySource from(CollectionTable collectionTable) {
		return collectionTable == null ? null : from( collectionTable.foreignKey() );
	}

	static ForeignKeySource inverseFrom(JoinTable joinTable) {
		return joinTable == null ? null : from( joinTable.inverseForeignKey() );
	}

	static ForeignKeySource from(SecondaryTable secondaryTable) {
		return secondaryTable == null ? null : from( secondaryTable.foreignKey() );
	}

	String name();

	ConstraintMode constraintMode();

	String definition();

	String options();

	default boolean isNoConstraint() {
		return constraintMode() == ConstraintMode.NO_CONSTRAINT;
	}

	record JpaForeignKeySource(ForeignKey foreignKey) implements ForeignKeySource {
		@Override
		public String name() {
			return foreignKey.name();
		}

		@Override
		public ConstraintMode constraintMode() {
			return foreignKey.value();
		}

		@Override
		public String definition() {
			return foreignKey.foreignKeyDefinition();
		}

		@Override
		public String options() {
			return foreignKey.options();
		}
	}
}
