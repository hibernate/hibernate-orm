/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorDB2DatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorDB2DatabaseImpl INSTANCE = new SequenceInformationExtractorDB2DatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "seqname";
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceSchemaColumn() {
		return "seqschema";
	}

	@Override
	protected String sequenceStartValueColumn() {
		return "start";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "minvalue";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "maxvalue";
	}
}
