/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.List;

import org.hibernate.Internal;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;

/// Internal selection-position tracking used during standard SQM conversion.
///
/// @author Steve Ebersole
@Internal
public interface SqmAliasedNodeCollector {
	void next();

	List<SqlSelection> getSelections(int position);
}
