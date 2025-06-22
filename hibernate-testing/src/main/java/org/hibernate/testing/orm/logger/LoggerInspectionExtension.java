/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.logger;

import java.util.Set;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.LogListener;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.logger.Triggerable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.jboss.logging.BasicLogger;

public class LoggerInspectionExtension implements AfterEachCallback {

	public static LoggerInspectionRuleBuilder builder() {
		return new LoggerInspectionRuleBuilder();
	}

	public static class LoggerInspectionRuleBuilder {
		BasicLogger log;

		public LoggerInspectionRuleBuilder setLogger(BasicLogger log) {
			this.log = log;
			return this;
		}

		public LoggerInspectionExtension build() {
			return new LoggerInspectionExtension( log );
		}
	}

	private final BasicLogger log;

	public LoggerInspectionExtension(BasicLogger log) {
		this.log = log;
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

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		LogInspectionHelper.clearAllListeners( log );
	}
}
