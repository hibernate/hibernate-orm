/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
@NamedQuery(name = "allMouse",
			query = "select m from ApplicationServer m")
package org.hibernate.orm.test.jpa.pack.war;
import org.hibernate.annotations.NamedQuery;
