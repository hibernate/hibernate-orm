/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.misc;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

/**
 * Defines how we rename a file being transformed.
 *
 * @apiNote All settings set to null indicates to use the same file
 * 		name.  Note that with no {@link TransformHbmXmlTask#getOutputDirectory()}
 * 		specified, this would mean replacing the original rather than making a
 * 		copy (effectively, {@link TransformHbmXmlTask#getDeleteHbmFiles `deleteHbmFiles=true`})
 */
abstract public class TransformationNaming {

	public TransformationNaming() {
	}

	/**
	 * A prefix to apply to the file name.
	 * <p>
	 * E.g. given an {@code `hbm.xml`} file named {@code `my-mappings.hbm.xml`}
	 * and a configured prefix of {@code `transformed-`}, the copy file's
	 * name would be {@code `transformed-my-mappings.hbm.xml`}
	 *
	 * @see #getExtension()
	 */
	@Input
	@Optional
	abstract public Property<String> getPrefix();

	/**
	 * A suffix to apply to the file name.
	 * <p>
	 * E.g. given an {@code `hbm.xml`} file named {@code `my-mappings.hbm.xml`}
	 * and a configured suffix of {@code `-transformed`}, the copy file's
	 * name would be {@code `my-mappings-transformed.hbm.xml`}
	 *
	 * @see #getExtension()
	 */
	@Input
	@Optional
	abstract public Property<String> getSuffix();

	@Input
	@Optional
	abstract public Property<String> getExtension();

	public boolean areNoneDefined() {
		return !getPrefix().isPresent() && !getSuffix().isPresent() && !getExtension().isPresent();
	}
}
