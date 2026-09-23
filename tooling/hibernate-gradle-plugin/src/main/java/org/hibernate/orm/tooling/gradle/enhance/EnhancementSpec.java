/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.enhance;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.util.ArrayList;

/**
 * DSL extension for configuring bytecode enhancement - available as `project.hibernate.enhancement { ... }`
 */
abstract public class EnhancementSpec {

	public EnhancementSpec() {
		getEnableLazyInitialization().convention( true );
		getEnableDirtyTracking().convention( true );
		getEnableAssociationManagement().convention( false );
		getClassNames().convention(new ArrayList<>());
	}

	/**
	 * Whether lazy-initialization handling should be incorporated into the enhanced bytecode
	 */
	@Deprecated(forRemoval = true)
	abstract public Property<Boolean> getEnableLazyInitialization();

	/**
	 * Whether dirty-tracking should be incorporated into the enhanced bytecode
	 */
	@Deprecated(forRemoval = true)
	abstract public Property<Boolean> getEnableDirtyTracking();

	/**
	 * Whether bidirectional association-management handling should be incorporated into the enhanced bytecode
	 */
	@Deprecated(forRemoval = true)
	abstract public Property<Boolean> getEnableAssociationManagement();

	/**
	 * Whether extended enhancement should be performed.
	 *
	 * @deprecated Use {@linkplain #getEnableClientEnhancement()} instead.
	 */
	@Deprecated(forRemoval = true)
	abstract public Property<Boolean> getEnableExtendedEnhancement();

	/// Enables client field-access enhancement. Defaults to false; takes precedence
	/// over the deprecated extended-enhancement alias when explicitly configured.
	abstract public Property<Boolean> getEnableClientEnhancement();

	/**
	 * Returns the classes on which enhancement needs to be done
	 */
	abstract public ListProperty<String> getClassNames();
}
