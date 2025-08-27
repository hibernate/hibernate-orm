/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@Jpa
@MessageKeyInspection( messageKey = "HHH000424", logger = @Logger( loggerNameClass = LobCreatorBuilderImpl.class) )
public class LobCreationCheckSkipTest {
	@Test
	public void test(MessageKeyWatcher watcher) {
		assertFalse( watcher.wasTriggered() );
	}
}
