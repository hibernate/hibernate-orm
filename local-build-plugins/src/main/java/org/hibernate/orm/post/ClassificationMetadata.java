/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

/// One versioned classification metadata document.
///
/// @author Steve Ebersole
public final class ClassificationMetadata {
	public static final String SCHEMA = "hibernate-orm-classifications";
	public static final int SCHEMA_VERSION = 1;

	private final String hibernateVersion;
	private final String sourceVersion;
	private final ClassificationModel model;

	public ClassificationMetadata(
			String hibernateVersion,
			String sourceVersion,
			ClassificationModel model) {
		this.hibernateVersion = hibernateVersion;
		this.sourceVersion = sourceVersion;
		this.model = model;
	}

	public String getHibernateVersion() {
		return hibernateVersion;
	}

	public String getSourceVersion() {
		return sourceVersion;
	}

	public ClassificationModel getModel() {
		return model;
	}
}
