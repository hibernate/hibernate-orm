/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.schema.spi.Exporter;

/**
 * @author Steve Ebersole
 */
public class StandardSequenceExporter implements Exporter<Sequence> {
	private final Dialect dialect;

	public StandardSequenceExporter(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public String[] getSqlCreateStrings(Sequence sequence, Metadata metadata, SqlStringGenerationContext context) {
		return dialect.getCreateSequenceStrings(
				getFormattedSequenceName( sequence.getName(), metadata, context ),
				sequence.getInitialValue(),
				sequence.getIncrementSize()
		);
	}

	@Override
	public String[] getSqlDropStrings(Sequence sequence, Metadata metadata, SqlStringGenerationContext context) {
		return dialect.getDropSequenceStrings(
				getFormattedSequenceName( sequence.getName(), metadata, context )
		);
	}

	protected String getFormattedSequenceName(QualifiedSequenceName name, Metadata metadata,
			SqlStringGenerationContext context) {
		return context.format( name );
	}
}
