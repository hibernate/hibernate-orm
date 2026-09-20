/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = @Setting(name = "hibernate.dialect",
		value = "org.hibernate.orm.test.where.annotations.RestrictedToOneEmulatedUpsertTest$EmulatedUpsertDialect"))
@RequiresDialect(H2Dialect.class)
class AssociationRestrictedToOneEmulatedUpsertTest extends AssociationRestrictedToOneTest {
}
