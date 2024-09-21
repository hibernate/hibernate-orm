/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Test package for metatata facilities
 * It contains an example of filter metadata
 */
@FilterDef(
		name = "betweenLength",
		defaultCondition = ":minLength <= length and :maxLength >= length",
		parameters = {
				@ParamDef(name = "minLength", type = Integer.class),
				@ParamDef(name = "maxLength", type = Integer.class)
		}
) package org.hibernate.orm.test.annotations.entity;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
