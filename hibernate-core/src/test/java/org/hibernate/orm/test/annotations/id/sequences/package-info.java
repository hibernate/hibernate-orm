/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

//$Id: package-info.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
/**
 * Test package for metatata facilities
 * It contains an example of package level metadata
 */
@org.hibernate.annotations.GenericGenerator(name = "system-uuid", strategy = "uuid")
@org.hibernate.annotations.GenericGenerators(
		@org.hibernate.annotations.GenericGenerator(name = "system-uuid-2", strategy = "uuid")
)
package org.hibernate.orm.test.annotations.id.sequences;
