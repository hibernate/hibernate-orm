/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.orm.test.immutable.entitywithmutablecollection.AbstractEntityWithOneToManyTest;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;


/**
 * @author Gail Badner
 */
@JiraKey(value = "HHH-4992")
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse/ContractVariationVersionedOneToManyJoin.hbm.xml"
)
public class VersionedEntityWithInverseOneToManyJoinTest extends AbstractEntityWithOneToManyTest {

	@Override
	protected boolean checkUpdateCountsAfterAddingExistingElement() {
		return false;
	}

	@Override
	protected boolean checkUpdateCountsAfterRemovingElementWithoutDelete() {
		return false;
	}
}
