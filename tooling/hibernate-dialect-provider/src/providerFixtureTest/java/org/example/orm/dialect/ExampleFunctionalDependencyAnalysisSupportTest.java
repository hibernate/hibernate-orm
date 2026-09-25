package org.example.orm.dialect;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies functional-dependency support supplied by the standalone provider.
///
/// @author Steve Ebersole
public class ExampleFunctionalDependencyAnalysisSupportTest {
	@Test
	void suppliesAProviderOwnedProfile() {
		assertEquals(
				FunctionalDependencyAnalysisSupport.TABLE_GROUP,
				new ExampleDialect().getFunctionalDependencyAnalysisSupport()
		);
	}
}
