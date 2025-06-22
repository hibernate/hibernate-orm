/*
 * SPDX-License-Identifier: Apache-2.0
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
