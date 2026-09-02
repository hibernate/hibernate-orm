/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider.internal;

/// Identifies why a provider-boundary finding was reported and defines its
/// default severity and corrective guidance.
///
/// @author Steve Ebersole
public enum ProviderBoundaryCause {
	INTERNAL_TARGET(
			Severity.WARNING,
			"Provider bytecode depends on an internal Hibernate ORM declaration.",
			"Replace the internal dependency with a documented API or SPI contract; internal declarations have no compatibility guarantee."
	),
	MISSING_IMPLEMENT_ROLE(
			Severity.ERROR,
			"Provider bytecode uses a Hibernate declaration as an implementation point without a supported source and target classification.",
			"Use a Hibernate SPI contract classified with IMPLEMENT. A genuine provider-owned SPI may compose Hibernate API when declared in an .spi package or with @SPI."
	);

	private final Severity severity;
	private final String message;
	private final String remediation;

	ProviderBoundaryCause(Severity severity, String message, String remediation) {
		this.severity = severity;
		this.message = message;
		this.remediation = remediation;
	}

	public Severity severity() {
		return severity;
	}

	public String message() {
		return message;
	}

	public String remediation() {
		return remediation;
	}

	/// Severity assigned to a provider-boundary cause.
	public enum Severity {
		WARNING,
		ERROR
	}
}
