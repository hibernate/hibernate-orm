package org.hibernate.dialect.sequence.internal;

import org.hibernate.dialect.sequence.spi.SequenceSupport;

import jakarta.annotation.Nullable;
import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.DB2zDialect}.
 *
 * @author Gavin King
 */
public final class DB2zSequenceSupport implements SequenceSupport {

	private static final SequenceSupport INSTANCE = new DB2zSequenceSupport();

	public static SequenceSupport getInstance() {
		return INSTANCE;
	}

	@Override
	public String getFromDual() {
		return " from sysibm.sysdummy1";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval for " + sequenceName;
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) {
		return "prevval for " + sequenceName;
	}

	@Override
	public @Nullable String getSequenceNextValParameterizedString(String sequenceName) throws MappingException {
		return "with cte as (select 1 v from sysibm.sysdummy1 t union all select t.v + 1 from cte t where t.v<?) select "
			+ getSelectSequenceNextValString( sequenceName ) + " from cte t";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName
				+ " as integer start with 1 increment by 1 minvalue 1 nomaxvalue nocycle nocache"; //simple default settings..
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		return "create sequence " + sequenceName
				+ " as integer start with " + initialValue
				+ " increment by " + incrementSize
				+ " minvalue 1 nomaxvalue nocycle nocache";
	}
}
