/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi.test.client;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;

/**
 * @author Brett Meyer
 */
public class TestStrategyRegistrationProvider implements StrategyRegistrationProvider {

	public static final String GREGORIAN = "gregorian";

	@SuppressWarnings("unchecked")
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		return Collections.singletonList(
				(StrategyRegistration) new SimpleStrategyRegistrationImpl(
						Calendar.class,
						GregorianCalendar.class,
						GREGORIAN
				)
		);
	}
}
