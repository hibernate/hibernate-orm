/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity.removal;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialectFeature(DialectChecks.SupportsCascadeDeleteCheck.class)
public class RemoveDefaultRevisionEntity extends AbstractRevisionEntityRemovalTest {
	@Override
	protected Class<?> getRevisionEntityClass() {
		return SequenceIdRevisionEntity.class;
	}
}
