/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;

/**
 * @author Steve Ebersole
 */
@Remove
public interface VersionAttributeSource
		extends SingularAttributeSource, RelationalValueSourceContainer, ImplicitBasicColumnNameSource {
	String getUnsavedValue();
	String getSource();
}
