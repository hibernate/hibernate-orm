/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;

/**
 * @author Steve Ebersole
 */
public interface GenerationOptions {
	Provider<Directory> getGenerationDirectory();
	Provider<Boolean> getApplyGeneratedAnnotation();
	SetProperty<String> getSuppressions();
}
