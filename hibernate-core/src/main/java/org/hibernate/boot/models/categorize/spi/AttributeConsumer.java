/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.internal.util.IndexedConsumer;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface AttributeConsumer extends IndexedConsumer<AttributeMetadata> {

}
