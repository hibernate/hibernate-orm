/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

/// Validates SPI role and implementation-point rules from canonical
/// classification metadata.
///
/// @author Steve Ebersole
public abstract class SpiValidationTask extends AbstractClassificationValidationTask {
	public SpiValidationTask() {
		super( "Validates the Hibernate provider SPI surface", "spi-validation.txt" );
	}

	@Override
	protected ValidationResult validate(ClassificationModel model, ValidationAllowlist allowlist) {
		return new SpiValidator().validate( model, allowlist );
	}

	@Override
	protected String title() {
		return "Hibernate ORM SPI validation";
	}
}
