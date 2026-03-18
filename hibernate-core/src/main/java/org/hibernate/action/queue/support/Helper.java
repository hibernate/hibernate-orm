/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.persister.entity.EntityPersister;


/**
 * @author Steve Ebersole
 */
public class Helper {
	public static boolean needsIdentityPrePhase(EntityPersister persister, Object identifier) {
		// IDENTITY generation needs pre-phase execution when ID is not yet assigned
		// (i.e., identifier == null means database will generate it)
		return persister.getGenerator().generatedOnExecution() && identifier == null;
	}
}
