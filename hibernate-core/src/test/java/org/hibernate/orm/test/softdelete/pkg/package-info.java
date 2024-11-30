/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Tests for {@link org.hibernate.annotations.SoftDelete}
 * applied to a package
 *
 * @author Steve Ebersole
 */
@SoftDelete(converter = YesNoConverter.class)
package org.hibernate.orm.test.softdelete.pkg;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;
