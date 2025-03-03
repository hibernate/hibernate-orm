/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines support for implicit ResultSet mappings.  Generally these
 * implicit mappings come from:<ul>
 *     <li>named scalar result mappings specifying no explicit columns</li>
 *     <li>mapping results to a named entity class with no explicit property mappings</li>
 * </ul>
 */
package org.hibernate.query.results.internal.implicit;
