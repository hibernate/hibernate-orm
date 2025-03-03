/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

import java.io.Serializable;

/**
 * Represents a JPA entity lifecycle callback method.
 * <p>
 * There are two flavors of this, which we abstract here:
 * <ul>
 * <li>an annotated method of the entity class itself, or
 * <li>an annotated method of a separate <em>entity listener</em> class
 *     identified via the {@link jakarta.persistence.EntityListeners}
 *     annotation.
 * </ul>
 *
 * @author Kabir Khan
 * @author Steve Ebersole
 */
public interface Callback extends Serializable {
	/**
	 * The type of callback (pre-update, pre-persist, etc) handled
	 */
	CallbackType getCallbackType();

	/**
	 * Contract for performing the callback
	 *
	 * @param entity Reference to the entity for which the callback is triggered.
	 */
	void performCallback(Object entity);
}
