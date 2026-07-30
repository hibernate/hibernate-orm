/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import org.hibernate.boot.model.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
@Remove
public interface JavaTypeDescriptorResolvable {
	void resolveJavaTypeDescriptor(JavaTypeDescriptor descriptor);
}
