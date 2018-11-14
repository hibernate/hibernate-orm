/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.logger;

import java.util.Set;

import org.jboss.logging.BasicLogger;

/**
 * @author Andrea Boriero
 */
public class LoggerInspectionExtension {

	private final BasicLogger log;

	public LoggerInspectionExtension(BasicLogger log) {
		this.log = log;
	}


	public void afterEach() {
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

	public Triggerable watchForLogMessages(Set<String> prefixes) {
		TriggerOnPrefixLogListener listener = new TriggerOnPrefixLogListener( prefixes );
		registerListener( listener );
		return listener;
	}
}
