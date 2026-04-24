/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.hbm;

/**
 * Global settings for the {@code <hibernate-mapping>} root element.
 *
 * @author Koen Aers
 */
public record HibernateMappingSettings(
		String defaultAccess,
		String defaultCascade,
		boolean defaultLazy,
		boolean autoImport,
		String schema,
		String catalog
) {

	public static HibernateMappingSettings defaults() {
		return new HibernateMappingSettings("property", "none", true, true, null, null);
	}

	public boolean hasNonDefaultAccess() {
		return defaultAccess != null && !defaultAccess.isEmpty()
				&& !"property".equals(defaultAccess);
	}

	public boolean hasNonDefaultCascade() {
		return defaultCascade != null && !defaultCascade.isEmpty()
				&& !"none".equals(defaultCascade);
	}

	public boolean hasSchema() {
		return schema != null && !schema.isEmpty();
	}

	public boolean hasCatalog() {
		return catalog != null && !catalog.isEmpty();
	}
}
