/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.state.internal;

/**
 * @author Gavin King
 *
 * @since 7.4
 */
public final class StandardStateManagement extends AbstractStateManagement {
	public static final StandardStateManagement INSTANCE = new StandardStateManagement();

	private StandardStateManagement() {
	}
}
