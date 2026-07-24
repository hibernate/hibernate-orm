/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.util;

import org.hibernate.models.Creator;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableClassDetails;

/**
 * Class details for a component class that reverse engineering intends to
 * generate later in the export process.
 *
 * @since 9.0
 * @author Steve Ebersole
 */
public final class GeneratedComponentClassDetails {

	private GeneratedComponentClassDetails() {
	}

	public static MutableClassDetails create(String componentClassName, ModelsContext modelsContext) {
		return Creator.createDynamicClassDetails(
				componentClassName,
				componentClassName,
				Object.class,
				false,
				null,
				null,
				modelsContext
		);
	}
}
