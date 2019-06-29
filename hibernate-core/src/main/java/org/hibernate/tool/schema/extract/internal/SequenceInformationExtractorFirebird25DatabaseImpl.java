/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;

/**
 * @author Mark Rotteveel
 */
public class SequenceInformationExtractorFirebird25DatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorFirebird25DatabaseImpl INSTANCE = new SequenceInformationExtractorFirebird25DatabaseImpl();

	@Override
	protected Long resultSetIncrementValue(ResultSet resultSet) {
		// increment is always 1
		return 1L;
	}

	@Override
	protected String sequenceNameColumn() {
		return "RDB$GENERATOR_NAME";
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
		return null;
	}

	@Override
	protected String sequenceMinValueColumn() {
		return null;
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return null;
	}

	@Override
	protected String sequenceIncrementColumn() {
		return null;
	}
}
