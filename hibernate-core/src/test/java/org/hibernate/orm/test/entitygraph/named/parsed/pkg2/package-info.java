/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * @author Steve Ebersole
 */
@NamedEntityGraph( name = "person-name", graph = "name")
package org.hibernate.orm.test.entitygraph.named.parsed.pkg2;

import org.hibernate.annotations.NamedEntityGraph;
