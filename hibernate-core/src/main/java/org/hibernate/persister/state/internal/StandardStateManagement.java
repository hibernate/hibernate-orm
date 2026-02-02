/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

import org.hibernate.Internal;

/**
 * @author Gavin King
 */
@Internal
public final class StandardStateManagement extends AbstractStateManagement {
	public static final StandardStateManagement INSTANCE = new StandardStateManagement();

	private StandardStateManagement() {
	}
}
