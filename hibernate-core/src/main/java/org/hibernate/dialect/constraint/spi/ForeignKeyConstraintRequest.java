/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.constraint.spi;

import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;
import static org.hibernate.internal.util.StringHelper.isBlank;

/// Describes one foreign-key constraint to be added to an existing table.
///
/// Use [#structured] for mapping-model foreign keys and [#explicit] for an
/// explicitly supplied SQL definition. Rendered identifiers and SQL fragments
/// are consumed verbatim.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public record ForeignKeyConstraintRequest(
		String constraintName,
		List<String> sourceColumnNames,
		@Nullable String referencedTableName,
		List<String> targetColumnNames,
		boolean referencesPrimaryKey,
		@Nullable String explicitDefinition) {

	public ForeignKeyConstraintRequest {
		if ( isBlank( constraintName ) ) {
			throw new IllegalArgumentException( "constraintName must not be blank" );
		}
		sourceColumnNames = List.copyOf( sourceColumnNames );
		targetColumnNames = List.copyOf( targetColumnNames );
		if ( explicitDefinition == null ) {
			if ( sourceColumnNames.isEmpty() ) {
				throw new IllegalArgumentException( "sourceColumnNames must not be empty" );
			}
			if ( isBlank( referencedTableName ) ) {
				throw new IllegalArgumentException( "referencedTableName must not be blank" );
			}
			if ( targetColumnNames.isEmpty() || sourceColumnNames.size() != targetColumnNames.size() ) {
				throw new IllegalArgumentException( "source and target column counts must match" );
			}
			validateNames( sourceColumnNames, "sourceColumnNames" );
			validateNames( targetColumnNames, "targetColumnNames" );
		}
		else {
			if ( isBlank( explicitDefinition ) ) {
				throw new IllegalArgumentException( "explicitDefinition must not be blank" );
			}
			if ( !sourceColumnNames.isEmpty() || !targetColumnNames.isEmpty()
					|| referencedTableName != null || referencesPrimaryKey ) {
				throw new IllegalArgumentException( "explicit foreign-key definitions must not contain structured values" );
			}
		}
	}

	public static ForeignKeyConstraintRequest structured(
			String constraintName,
			List<String> sourceColumnNames,
			String referencedTableName,
			List<String> targetColumnNames,
			boolean referencesPrimaryKey) {
		return new ForeignKeyConstraintRequest(
				constraintName,
				sourceColumnNames,
				referencedTableName,
				targetColumnNames,
				referencesPrimaryKey,
				null
		);
	}

	public static ForeignKeyConstraintRequest explicit(String constraintName, String explicitDefinition) {
		return new ForeignKeyConstraintRequest(
				constraintName,
				List.of(),
				null,
				List.of(),
				false,
				explicitDefinition
		);
	}

	public boolean isExplicitDefinition() {
		return explicitDefinition != null;
	}

	private static void validateNames(List<String> names, String role) {
		for ( String name : names ) {
			if ( isBlank( name ) ) {
				throw new IllegalArgumentException( role + " must not contain blank names" );
			}
		}
	}
}
