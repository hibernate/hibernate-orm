/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
