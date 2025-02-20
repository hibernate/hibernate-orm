/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Tests for {@link org.hibernate.annotations.SoftDelete}
 * applied at the package level and overridden at the entity and
 * collection levels
 *
 * @author Steve Ebersole
 */
@SoftDelete(converter = YesNoConverter.class)
package org.hibernate.orm.test.softdelete.discovery.override;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;
