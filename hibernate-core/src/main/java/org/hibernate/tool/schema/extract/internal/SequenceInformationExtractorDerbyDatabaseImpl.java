/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

/**
 * @author Vlad Mihalcea
 */
public class SequenceInformationExtractorDerbyDatabaseImpl extends SequenceInformationExtractorLegacyImpl {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorDerbyDatabaseImpl INSTANCE = new SequenceInformationExtractorDerbyDatabaseImpl();

	@Override
	protected String sequenceNameColumn() {
		return "sequencename";
	}

	@Override
	protected String sequenceCatalogColumn() {
		return null;
	}

	@Override
	protected String sequenceStartValueColumn() {
		return "startvalue";
	}

	@Override
	protected String sequenceMinValueColumn() {
		return "minimumvalue";
	}

	@Override
	protected String sequenceMaxValueColumn() {
		return "maximumvalue";
	}
}
