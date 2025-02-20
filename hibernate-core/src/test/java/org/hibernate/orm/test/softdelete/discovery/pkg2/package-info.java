/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Tests for {@link org.hibernate.annotations.SoftDelete}
 * applied to a package
 *
 * @author Steve Ebersole
 */
@SoftDelete(columnName="gone", converter = CustomTrueFalseConverter.class)
package org.hibernate.orm.test.softdelete.discovery.pkg2;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.orm.test.softdelete.CustomTrueFalseConverter;
