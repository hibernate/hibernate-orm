/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
