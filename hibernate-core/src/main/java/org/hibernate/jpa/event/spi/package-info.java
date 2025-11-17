/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * The SPI contracts for supporting JPA lifecycle callbacks and
 * {@link jakarta.persistence.EntityListeners entity listeners}.
 *
 * @see jakarta.persistence.EntityListeners
 * @see jakarta.persistence.PrePersist
 * @see jakarta.persistence.PreUpdate
 * @see jakarta.persistence.PreRemove
 * @see jakarta.persistence.PostPersist
 * @see jakarta.persistence.PostUpdate
 * @see jakarta.persistence.PostRemove
 * @see jakarta.persistence.PostLoad
 */
package org.hibernate.jpa.event.spi;
