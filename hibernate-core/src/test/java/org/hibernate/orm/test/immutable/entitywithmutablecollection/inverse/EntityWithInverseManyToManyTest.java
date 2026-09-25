package org.hibernate.orm.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.orm.test.immutable.entitywithmutablecollection.AbstractEntityWithManyToManyTest;

import org.hibernate.testing.orm.junit.DomainModel;

/**
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse/ContractVariation.hbm.xml"
)
public class EntityWithInverseManyToManyTest extends AbstractEntityWithManyToManyTest {
}
