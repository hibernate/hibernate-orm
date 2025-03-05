/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.hint;

import org.hibernate.dialect.Dialect;

/**
 * @deprecated Moved to {@link org.hibernate.dialect.Dialect}
 */
@Deprecated(since = "7.0", forRemoval = true)
public class IndexQueryHintHandler implements QueryHintHandler {

	@Override
	public String addQueryHints(String query, String hints) {
		return Dialect.addUseIndexQueryHint( query, hints);
	}
}
