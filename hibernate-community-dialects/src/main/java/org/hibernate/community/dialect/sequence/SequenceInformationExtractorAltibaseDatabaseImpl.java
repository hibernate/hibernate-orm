/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.community.dialect.sequence;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;

/**
 * An SequenceInfomation for Altibase
 *
 * @author Geoffrey Park
 */
public class SequenceInformationExtractorAltibaseDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorAltibaseDatabaseImpl INSTANCE = new SequenceInformationExtractorAltibaseDatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "sequence_name";
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return null;
	}

	@Override
	protected String sequenceStartValueColumn() {
		return "start_value";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "min_value";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "max_value";
	}

	@Override
	protected String sequenceIncrementColumn() {
		return "increment_by";
	}

}
