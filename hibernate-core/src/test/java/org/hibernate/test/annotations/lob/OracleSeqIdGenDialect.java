package org.hibernate.test.annotations.lob;

import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.id.SequenceIdentityGenerator;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OracleSeqIdGenDialect extends Oracle10gDialect {
	@Override
	public Class<?> getNativeIdentifierGeneratorClass() {
		return SequenceIdentityGenerator.class;
	}
}
