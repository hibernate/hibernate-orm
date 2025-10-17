/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = { "/org/hibernate/orm/test/mapping/collections/custom/parameterized/Mapping.hbm.xml" }
)
public class ParameterizedUserCollectionTypeHbmVariantTest extends ParameterizedUserCollectionTypeTest {
}
