/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
