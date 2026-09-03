/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.SPI;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Standard exporter for relational [Sequence] definitions.
///
/// Extend this class only to customize qualification of a sequence name.
/// Retain the supplied Dialect for the lifetime of this exporter and supply the
/// exporter from [Dialect#getSequenceExporter()].
///
/// @see Dialect#getSequenceExporter()
/// @see org.hibernate.dialect.sequence.spi.SequenceSupport
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public class StandardSequenceExporter implements Exporter<Sequence> {
	private final Dialect dialect;

	/// Create a standard sequence exporter owned by `dialect`.
	///
	/// @since 8.0
	@SPI(IMPLEMENT)
	public StandardSequenceExporter(Dialect dialect) {
		if ( dialect == null ) {
			throw new IllegalArgumentException( "dialect must not be null" );
		}
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

	/// Format the sequence name used by create and drop commands.
	protected String getFormattedSequenceName(QualifiedSequenceName name, Metadata metadata,
			SqlStringGenerationContext context) {
		return context.format( name );
	}
}
