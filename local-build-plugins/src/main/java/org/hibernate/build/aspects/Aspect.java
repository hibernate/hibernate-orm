/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.aspects;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * An aspect which can be applied to a Project
 *
 * @author Steve Ebersole
 */
public interface Aspect extends Plugin<Project> {
}
