/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.enhance;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.util.ArrayList;

/**
 * DSL extension for configuring bytecode enhancement - available as `project.hibernateOrm.enhancement`
 */
abstract public class EnhancementSpec {

	public EnhancementSpec() {
		getEnableLazyInitialization().convention( true );
		getEnableDirtyTracking().convention( true );
		getEnableAssociationManagement().convention( false );
		getEnableExtendedEnhancement().convention( false );
		getClassNames().convention(new ArrayList<>());
	}

	/**
	 * Whether lazy-initialization handling should be incorporated into the enhanced bytecode
	 */
	abstract public Property<Boolean> getEnableLazyInitialization();

	/**
	 * Whether dirty-tracking should be incorporated into the enhanced bytecode
	 */
	abstract public Property<Boolean> getEnableDirtyTracking();

	/**
	 * Whether bidirectional association-management handling should be incorporated into the enhanced bytecode
	 */
	abstract public Property<Boolean> getEnableAssociationManagement();

	/**
	 * Whether extended enhancement should be performed.
	 */
	abstract public Property<Boolean> getEnableExtendedEnhancement();

	/**
	 * Returns the classes on which enhancement needs to be done
	 */
	abstract public ListProperty<String> getClassNames();
}
