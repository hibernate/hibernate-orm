/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Package for processing JDBC {@code ResultSet}s into hydrated domain model graphs
 * based on a "load plan" defined by a "domain result graph", that is, one or more
 * {@link org.hibernate.sql.results.graph.DomainResult} nodes with zero or more
 * {@link org.hibernate.sql.results.graph.Fetch} nodes.
 */
@Incubating
package org.hibernate.sql.results;

import org.hibernate.Incubating;
