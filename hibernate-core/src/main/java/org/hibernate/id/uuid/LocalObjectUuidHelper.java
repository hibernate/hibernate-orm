/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.uuid;

import java.util.UUID;

/**
 * @author Steve Ebersole
 */
public class LocalObjectUuidHelper {
	private LocalObjectUuidHelper() {
	}

	public static String generateLocalObjectUuid() {
		return UUID.randomUUID().toString();
	}
}
