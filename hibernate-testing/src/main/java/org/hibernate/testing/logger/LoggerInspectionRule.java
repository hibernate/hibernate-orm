/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.logger;

import org.junit.rules.ExternalResource;

import org.jboss.logging.BasicLogger;

public final class LoggerInspectionRule extends ExternalResource {

	private final BasicLogger log;

	public LoggerInspectionRule(BasicLogger log) {
		this.log = log;
	}

	@Override
	protected void before() throws Throwable {
		// do nothing
	}

	@Override
	protected void after() {
		LogInspectionHelper.clearAllListeners( log );
	}

	public void registerListener(LogListener listener) {
		LogInspectionHelper.registerListener( listener, log );
	}

	public Triggerable watchForLogMessages(String prefix) {
		TriggerOnPrefixLogListener listener = new TriggerOnPrefixLogListener( prefix );
		registerListener( listener );
		return listener;
	}

}
