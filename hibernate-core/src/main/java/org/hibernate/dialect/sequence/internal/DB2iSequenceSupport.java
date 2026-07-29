package org.hibernate.dialect.sequence.internal;

import org.hibernate.dialect.sequence.spi.SequenceSupport;

import jakarta.annotation.Nullable;
import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.DB2iDialect}.
 *
 * @author Christian Beikov
 */
public class DB2iSequenceSupport implements SequenceSupport {

	private static final SequenceSupport INSTANCE = new DB2iSequenceSupport();

	public static SequenceSupport getInstance() {
		return INSTANCE;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval for " + sequenceName;
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "prevval for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "values " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getSequencePreviousValString(String sequenceName) throws MappingException {
		return "values " + getSelectSequencePreviousValString( sequenceName );
	}

	@Override
	public @Nullable String getSequenceNextValParameterizedString(String sequenceName) throws MappingException {
		return "with cte as (select 1 v from sysibm.sysdummy1 t union all select t.v + 1 from cte t where t.v<?) select "
			+ getSelectSequenceNextValString( sequenceName ) + " from cte t";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}
}
