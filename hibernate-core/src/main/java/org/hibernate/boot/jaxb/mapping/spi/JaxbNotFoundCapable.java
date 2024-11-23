/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import org.hibernate.annotations.NotFoundAction;

/**
 * @author Steve Ebersole
 */
public interface JaxbNotFoundCapable extends JaxbPersistentAttribute {
	NotFoundAction getNotFound();
	void setNotFound(NotFoundAction value);
}
