/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.env;

import org.gradle.api.Project;

import javax.inject.Inject;

/**
 * @author Steve Ebersole
 */
public class OrmBuildConfig {
	@Inject
	public OrmBuildConfig(Project project) {
	}
}
