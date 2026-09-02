/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.spi.StandardSequenceExporter;

/// Demonstrates a provider specialization of standard sequence-name
/// formatting.
///
/// @author Steve Ebersole
public final class ExampleSequenceExporter extends StandardSequenceExporter {
	public ExampleSequenceExporter(Dialect dialect) {
		super( dialect );
	}

	@Override
	protected String getFormattedSequenceName(
			QualifiedSequenceName name,
			Metadata metadata,
			SqlStringGenerationContext context) {
		return "fixture_" + name.getSequenceName().getText();
	}
}
