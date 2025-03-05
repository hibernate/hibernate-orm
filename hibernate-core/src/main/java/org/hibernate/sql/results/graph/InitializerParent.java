/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

/**
 * Provides access to information about the owner/parent of a fetch
 * in relation to the current "row" being processed.
 *
 * @author Steve Ebersole
 */
public interface InitializerParent<Data extends InitializerData> extends Initializer<Data> {

}
