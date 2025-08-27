/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

/**
 * @author Steve Ebersole
 */
public class ParameterizedUserCollectionTypeHbmVariantTest extends ParameterizedUserCollectionTypeTest {
	public String[] getMappings() {
		return new String[] { "mapping/collections/custom/parameterized/Mapping.hbm.xml" };
	}
}
