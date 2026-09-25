package org.hibernate.orm.integrationtest.java.module.test.listener;

import jakarta.persistence.PrePersist;

public class ModuleListener {
	@PrePersist
	public void prePersist(Object entity) {
		EventTracker.events.add( "module:" + entity.getClass().getSimpleName() );
	}
}
