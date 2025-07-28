/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;

import static org.hibernate.internal.log.DeprecationLogger.DEPRECATION_LOGGER;

public class EnhancementContext extends DefaultEnhancementContext {

	private ClassLoader classLoader = null;
	private boolean enableAssociationManagement = false;
	private boolean enableDirtyTracking = false;
	private boolean enableLazyInitialization = false;
	private boolean enableExtendedEnhancement = false;

	public EnhancementContext(
			ClassLoader classLoader,
			boolean enableAssociationManagement,
			boolean enableDirtyTracking,
			boolean enableLazyInitialization,
			boolean enableExtendedEnhancement) {
		this.classLoader = classLoader;
		this.enableAssociationManagement = enableAssociationManagement;
		this.enableDirtyTracking = enableDirtyTracking;
		this.enableLazyInitialization = enableLazyInitialization;
		this.enableExtendedEnhancement = enableExtendedEnhancement;
	}

	@Override
	public ClassLoader getLoadingClassLoader() {
		return classLoader;
	}

	@Override
	public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
		return enableAssociationManagement;
	}

	@Override
	public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
		return enableDirtyTracking;
	}

	@Override
	public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
		return enableLazyInitialization;
	}

	@Override
	public boolean isLazyLoadable(UnloadedField field) {
		return enableLazyInitialization;
	}

	@Override
	public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
		if (enableExtendedEnhancement) {
			DEPRECATION_LOGGER.deprecatedSettingForRemoval("extended enhancement", "false");
		}
		return enableExtendedEnhancement;
	}

}
