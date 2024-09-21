/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * @author Andrea Boriero
 */
public interface Resolvable {

	boolean resolve(MetadataBuildingContext buildingContext);

	BasicValue.Resolution<?> resolve();
}
