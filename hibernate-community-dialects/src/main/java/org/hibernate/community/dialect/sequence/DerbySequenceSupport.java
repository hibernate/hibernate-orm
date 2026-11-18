package org.hibernate.community.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.spi.DB2SequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.DerbyDialect}.
 *
 * @author Christian Beikov
 */
public final class DerbySequenceSupport extends DB2SequenceSupport {

	public static final SequenceSupport INSTANCE = new DerbySequenceSupport();

	@Override
	public boolean supportsSelectNextValParameterized() {
		// Derby supports neither WITH nor sysibm.sysdummy1, so the
		// recursive CTE used by DB2SequenceSupport can't be used
		return false;
	}

	@Override
	public @jakarta.annotation.Nullable String getSequenceNextValParameterizedString(String sequenceName) {
		return null;
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "SYSCS_UTIL.SYSCS_PEEK_AT_SEQUENCE('HIBERNATE_ORM_TEST','" + sequenceName.toUpperCase() + "')";
	}
}
