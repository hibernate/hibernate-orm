/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.dialect.testing.DialectTestKit;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

/// Compiled example of the manual test-kit bridge used by non-Gradle builds.
///
/// The class is abstract so Hibernate's Gradle fixture executes the generated
/// bridge instead of executing this documentation example a second time.
///
/// @author Steve Ebersole
abstract class ManualDialectContractTestExample {
	// tag::contract-suite[]
	@TestFactory
	DynamicContainer dialectContracts() {
		return DialectTestKit.contractTests( new ExampleDialectContractProfile() );
	}
	// end::contract-suite[]
}
