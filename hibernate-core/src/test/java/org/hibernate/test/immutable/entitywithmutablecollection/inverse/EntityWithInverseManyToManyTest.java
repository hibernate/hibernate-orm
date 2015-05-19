/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.immutable.entitywithmutablecollection.inverse;

import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithManyToManyTest;

/**
 * @author Gail Badner
 */
public class EntityWithInverseManyToManyTest extends AbstractEntityWithManyToManyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "immutable/entitywithmutablecollection/inverse/ContractVariation.hbm.xml" };
	}
}
