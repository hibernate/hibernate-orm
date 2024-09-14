/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jdbc.env;

import org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertFalse;

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
