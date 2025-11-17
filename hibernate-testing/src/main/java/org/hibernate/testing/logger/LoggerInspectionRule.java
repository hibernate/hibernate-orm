/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.logger;

import java.util.Set;

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

	public Triggerable watchForLogMessages(Set<String> prefixes) {
		TriggerOnPrefixLogListener listener = new TriggerOnPrefixLogListener( prefixes );
		registerListener( listener );
		return listener;
	}

}
