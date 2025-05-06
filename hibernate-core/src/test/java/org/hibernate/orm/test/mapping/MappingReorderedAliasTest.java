/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Brett Meyer
 */
@DomainModel(
		annotatedClasses = { Table2.class, Table1.class, ConfEntity.class, UserConfEntity.class, UserEntity.class }
)
public class MappingReorderedAliasTest extends AliasTest {
}
