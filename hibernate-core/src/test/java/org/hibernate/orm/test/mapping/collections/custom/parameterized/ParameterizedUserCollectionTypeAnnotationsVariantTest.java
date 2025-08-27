/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

/**
 * @author Steve Ebersole
 */
public class ParameterizedUserCollectionTypeAnnotationsVariantTest extends ParameterizedUserCollectionTypeTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entity.class };
	}
}
