/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.function.spi.ExpressionCoercionSupport.Requirement.CAST_INTEGER_DIVISION_TO_FLOAT;
import static org.hibernate.dialect.function.spi.ExpressionCoercionSupport.Requirement.CAST_NON_STRING_CONCATENATION_ARGUMENTS;

/// Verifies every nonstandard community expression-coercion profile and
/// inherited Dialect-family value.
///
/// @author Steve Ebersole
public class ExpressionCoercionSupportTest {
	@Test
	void concatenationCastRequirementPreservesEveryFamilyValue() {
		for ( Dialect dialect : List.of(
				new DB2LegacyDialect(),
				new DB2iLegacyDialect(),
				new DB2zLegacyDialect(),
				new DerbyDialect(),
				new DerbyLegacyDialect(),
				new SQLServerLegacyDialect(),
				new SybaseLegacyDialect(),
				new SybaseASELegacyDialect(),
				new SybaseAnywhereDialect() ) ) {
			assertRequirements( dialect, CAST_NON_STRING_CONCATENATION_ARGUMENTS );
		}
	}

	@Test
	void integerDivisionCastRequirementPreservesEveryCommunityValue() {
		assertRequirements( new HSQLLegacyDialect(), CAST_INTEGER_DIVISION_TO_FLOAT );
		assertRequirements( new InterSystemsIRISDialect(), CAST_INTEGER_DIVISION_TO_FLOAT );
	}

	private static void assertRequirements(
			Dialect dialect,
			ExpressionCoercionSupport.Requirement... requirements) {
		assertThat( dialect.getExpressionCoercionSupport().getRequirements() )
				.as( dialect.getClass().getSimpleName() )
				.containsExactlyInAnyOrder( requirements );
	}
}
