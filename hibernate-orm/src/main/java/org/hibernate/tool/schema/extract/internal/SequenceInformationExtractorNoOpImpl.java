/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.SQLException;
import java.util.Collections;

import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * @author Steve Ebersole
 */
public class SequenceInformationExtractorNoOpImpl implements SequenceInformationExtractor {
	/**
	 * Singleton access
	 */
	public static final SequenceInformationExtractorNoOpImpl INSTANCE = new SequenceInformationExtractorNoOpImpl();

	@Override
	@SuppressWarnings("unchecked")
	public Iterable<SequenceInformation> extractMetadata(ExtractionContext extractionContext) throws SQLException {
		return Collections.emptyList();
	}
}
