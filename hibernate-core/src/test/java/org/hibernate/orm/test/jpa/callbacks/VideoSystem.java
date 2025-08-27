/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
@ExcludeSuperclassListeners
@EntityListeners({FirstOneListener.class, IncreaseListener.class})
@MappedSuperclass
public class VideoSystem extends CommunicationSystem {
	public transient int counter = 0;
	@PreUpdate
	public void increase() {
		isFirst = false;
		isLast = false;
		communication++;
	}

	@PrePersist
	public void prepareEntity() {
		counter++;
	}
}
