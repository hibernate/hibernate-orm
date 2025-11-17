/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Tests for {@link org.hibernate.annotations.SoftDelete}
 * applied to a package
 *
 * @author Steve Ebersole
 */
@SoftDelete(converter = YesNoConverter.class)
package org.hibernate.orm.test.softdelete.discovery.pkg;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;
