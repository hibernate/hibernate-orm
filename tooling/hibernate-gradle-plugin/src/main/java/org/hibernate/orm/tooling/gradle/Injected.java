/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.api.internal.file.FileOperations;

import javax.inject.Inject;

public interface Injected {
	@Inject
	FileOperations getFileOperations();
}
