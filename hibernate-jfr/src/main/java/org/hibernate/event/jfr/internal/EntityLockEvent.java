/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.jfr.internal;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import org.hibernate.LockMode;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.internal.build.AllowNonPortable;

@Name(EntityLockEvent.NAME)
@Label("Entity Delete")
@Category("Hibernate ORM")
@Description("Entity Delete")
@StackTrace
@AllowNonPortable
public class EntityLockEvent extends Event implements DiagnosticEvent {
	public static final String NAME = "org.hibernate.orm.EntityDeleteEvent";

	@Label("Session Identifier")
	public String sessionIdentifier;

	@Label("Entity Identifier")
	public String id;

	@Label("Entity Name")
	public String entityName;

	@Label("Lock Mode")
	public LockMode lockMode;

	@Label("Success")
	public boolean success;

	@Override
	public String toString() {
		return NAME;
	}

}
