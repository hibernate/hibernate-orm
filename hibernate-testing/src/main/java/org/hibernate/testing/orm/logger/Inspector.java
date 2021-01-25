/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.logger;

import java.util.Set;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.LogListener;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.logger.Triggerable;

import org.jboss.logging.BasicLogger;

public class Inspector {
	private BasicLogger log;

	public void init(BasicLogger logger){
		log = logger;
	}

	public BasicLogger getLog(){
		return log;
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

	public void registerListener(LogListener listener) {
		LogInspectionHelper.registerListener( listener, log );
	}
}
