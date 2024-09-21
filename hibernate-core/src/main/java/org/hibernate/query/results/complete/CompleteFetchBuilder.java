/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.complete;

import java.util.List;

import org.hibernate.query.results.FetchBuilder;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface CompleteFetchBuilder extends FetchBuilder, ModelPartReference {
	@Override
	Fetchable getReferencedPart();

	List<String> getColumnAliases();
}
