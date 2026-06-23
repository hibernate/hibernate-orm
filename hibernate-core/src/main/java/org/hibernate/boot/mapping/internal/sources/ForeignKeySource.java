/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import java.util.List;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
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
	static ForeignKeySource noConstraint() {
		return new NoConstraintForeignKeySource();
	}

	static ForeignKeySource from(ForeignKey foreignKey) {
		return foreignKey == null ? null : new JpaForeignKeySource( foreignKey );
	}

	static ForeignKeySource from(JoinColumn joinColumn) {
		return joinColumn == null ? null : from( joinColumn.foreignKey() );
	}

	static ForeignKeySource from(JoinColumns joinColumns) {
		return joinColumns == null ? null : from( joinColumns.foreignKey() );
	}

	static ForeignKeySource from(MapKeyJoinColumn joinColumn) {
		return joinColumn == null ? null : from( joinColumn.foreignKey() );
	}

	static ForeignKeySource from(MapKeyJoinColumns joinColumns) {
		return joinColumns == null ? null : from( joinColumns.foreignKey() );
	}

	static ForeignKeySource from(AssociationOverride associationOverride) {
		return associationOverride == null ? null : from( associationOverride.foreignKey() );
	}

	static ForeignKeySource from(PrimaryKeyJoinColumn joinColumn) {
		return joinColumn == null ? null : from( joinColumn.foreignKey() );
	}

	static ForeignKeySource from(PrimaryKeyJoinColumns joinColumns) {
		return joinColumns == null ? null : from( joinColumns.foreignKey() );
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

	static ForeignKeySource firstSpecified(ForeignKeySource... foreignKeySources) {
		for ( ForeignKeySource foreignKeySource : foreignKeySources ) {
			if ( foreignKeySource != null && foreignKeySource.isSpecified() ) {
				return foreignKeySource;
			}
		}
		return null;
	}

	static ForeignKeySource fromFirstSpecifiedJoinColumn(List<JoinColumn> joinColumns) {
		for ( JoinColumn joinColumn : joinColumns ) {
			final ForeignKeySource foreignKeySource = from( joinColumn );
			if ( foreignKeySource != null && foreignKeySource.isSpecified() ) {
				return foreignKeySource;
			}
		}
		return null;
	}

	static ForeignKeySource fromFirstSpecifiedMapKeyJoinColumn(List<MapKeyJoinColumn> joinColumns) {
		for ( MapKeyJoinColumn joinColumn : joinColumns ) {
			final ForeignKeySource foreignKeySource = from( joinColumn );
			if ( foreignKeySource != null && foreignKeySource.isSpecified() ) {
				return foreignKeySource;
			}
		}
		return null;
	}

	static ForeignKeySource fromFirstSpecifiedPrimaryKeyJoinColumn(PrimaryKeyJoinColumn[] joinColumns) {
		for ( PrimaryKeyJoinColumn joinColumn : joinColumns ) {
			final ForeignKeySource foreignKeySource = from( joinColumn );
			if ( foreignKeySource != null && foreignKeySource.isSpecified() ) {
				return foreignKeySource;
			}
		}
		return null;
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

	default boolean isSpecified() {
		return constraintMode() != ConstraintMode.PROVIDER_DEFAULT
				|| notEmpty( name() )
				|| notEmpty( definition() )
				|| notEmpty( options() );
	}

	private static boolean notEmpty(String value) {
		return value != null && !value.isEmpty();
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

	record NoConstraintForeignKeySource() implements ForeignKeySource {
		@Override
		public String name() {
			return "";
		}

		@Override
		public ConstraintMode constraintMode() {
			return ConstraintMode.NO_CONSTRAINT;
		}

		@Override
		public String definition() {
			return "";
		}

		@Override
		public String options() {
			return "";
		}
	}
}
