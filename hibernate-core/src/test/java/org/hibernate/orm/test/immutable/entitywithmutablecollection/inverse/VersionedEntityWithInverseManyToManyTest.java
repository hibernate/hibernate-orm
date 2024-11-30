/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.orm.test.immutable.entitywithmutablecollection.AbstractEntityWithManyToManyTest;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse/ContractVariationVersioned.hbm.xml"
)
public class VersionedEntityWithInverseManyToManyTest extends AbstractEntityWithManyToManyTest {
}
