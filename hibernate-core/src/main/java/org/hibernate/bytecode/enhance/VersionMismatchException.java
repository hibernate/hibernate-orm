/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance;

import java.util.Locale;

import org.hibernate.bytecode.enhance.spi.EnhancementException;

import net.bytebuddy.description.type.TypeDescription;

/**
 * Indicates that the version of Hibernate used to enhance
 * a class is different from the version being used at runtime.
 *
 * @author Steve Ebersole
 */
public class VersionMismatchException extends EnhancementException {
	private final String typeName;
	private final String enhancementVersion;
	private final String runtimeVersion;

	public VersionMismatchException(
			TypeDescription typeDescription,
			String enhancementVersion,
			String runtimeVersion) {
		super(
				String.format(
						Locale.ROOT,
						"Mismatch between Hibernate version used for bytecode enhancement (%s) and runtime (%s) for `%s`",
						enhancementVersion,
						runtimeVersion,
						typeDescription.getName()
				)
		);

		this.typeName = typeDescription.getName();
		this.enhancementVersion = enhancementVersion;
		this.runtimeVersion = runtimeVersion;
	}

	public String getTypeName() {
		return typeName;
	}

	public String getEnhancementVersion() {
		return enhancementVersion;
	}

	public String getRuntimeVersion() {
		return runtimeVersion;
	}
}
