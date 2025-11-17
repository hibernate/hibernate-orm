/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.internal;

import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.internal.build.AllowNonPortable;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name(DirtyCalculationEvent.NAME)
@Label("DirtyCalculationEvent Execution")
@Category("Hibernate ORM")
@Description("DirtyCalculationEvent Execution")
@StackTrace
@AllowNonPortable
public class DirtyCalculationEvent extends Event implements DiagnosticEvent {
	public static final String NAME = "org.hibernate.orm.DirtyCalculationEvent";

	@Label("Session Identifier")
	public String sessionIdentifier;

	@Label("Entity Name")
	public String entityName;

	@Label("Entity Status")
	public String entityStatus;

	@Label("Found properties")
	public boolean dirty;

	@Override
	public String toString() {
		return NAME;
	}

}
