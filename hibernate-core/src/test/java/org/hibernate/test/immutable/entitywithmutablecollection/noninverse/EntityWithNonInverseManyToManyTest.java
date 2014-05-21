/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.immutable.entitywithmutablecollection.noninverse;

import org.hibernate.test.immutable.entitywithmutablecollection.AbstractEntityWithManyToManyTest;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;

/**
 * @author Gail Badner
 */
@FailureExpectedWithNewUnifiedXsd(message = "mapping looks off -- shouldn't Contract#parties be inverse?")
public class EntityWithNonInverseManyToManyTest extends AbstractEntityWithManyToManyTest {
	@Override
	public String[] getMappings() {
//		return new String[] { "immutable/entitywithmutablecollection/noninverse/ContractVariation.hbm.xml" };
		// TODO: force it to blow up -- some of the abstract methods pass, so the builds will fail w/o this
		return null;
	}
}
