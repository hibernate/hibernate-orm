/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * An {@link Exporter} for {@linkplain Sequence sequences}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.dialect.sequence.SequenceSupport
 */
public class StandardSequenceExporter implements Exporter<Sequence> {
	private final Dialect dialect;

	public StandardSequenceExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Sequence sequence, Metadata metadata, SqlStringGenerationContext context) {
		return dialect.getSequenceSupport().getCreateSequenceStrings(
				getFormattedSequenceName( sequence.getName(), metadata, context ),
				sequence.getInitialValue(),
				sequence.getIncrementSize(),
				sequence.getOptions()
		);
	}

	@Override
	public String[] getSqlDropStrings(Sequence sequence, Metadata metadata, SqlStringGenerationContext context) {
		return dialect.getSequenceSupport().getDropSequenceStrings(
				getFormattedSequenceName( sequence.getName(), metadata, context )
		);
	}

	protected String getFormattedSequenceName(QualifiedSequenceName name, Metadata metadata,
			SqlStringGenerationContext context) {
		return context.format( name );
	}
}
