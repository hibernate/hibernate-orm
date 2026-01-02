/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

@NamedEntityGraph(root = Duplicator.class, name = "duplicated-name", graph = "name")
package org.hibernate.orm.test.entitygraph.named.parsed.pckgwithgraphnameduplication;

import org.hibernate.annotations.NamedEntityGraph;
