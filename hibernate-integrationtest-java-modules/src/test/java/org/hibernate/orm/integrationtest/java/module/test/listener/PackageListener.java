package org.hibernate.orm.integrationtest.java.module.test.listener;

import jakarta.persistence.PrePersist;

public class PackageListener {
	@PrePersist
	public void prePersist(Object entity) {
		EventTracker.events.add( "package:" + entity.getClass().getSimpleName() );
	}
}
