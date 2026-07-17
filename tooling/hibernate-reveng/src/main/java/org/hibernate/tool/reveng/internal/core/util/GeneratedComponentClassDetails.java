/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.util;

import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassLoading;
import org.hibernate.models.spi.ModelsContext;

/**
 * Class details for a component class that reverse engineering intends to
 * generate later in the export process.
 *
 * @since 9.0
 * @author Steve Ebersole
 */
public class GeneratedComponentClassDetails extends DynamicClassDetails {

	public GeneratedComponentClassDetails(String componentClassName, ModelsContext modelsContext) {
		super( componentClassName, componentClassName, Object.class, false, null, null, modelsContext );
	}

	@Override
	public <X> Class<X> toJavaClass() {
		return objectClass();
	}

	@Override
	public <X> Class<X> toJavaClass(ClassLoading classLoading, ModelsContext modelContext) {
		return objectClass();
	}

	@SuppressWarnings("unchecked")
	private static <X> Class<X> objectClass() {
		return (Class<X>) Object.class;
	}
}
