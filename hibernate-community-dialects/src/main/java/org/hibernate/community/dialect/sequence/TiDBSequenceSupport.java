package org.hibernate.community.dialect.sequence;

import jakarta.annotation.Nullable;
import org.hibernate.MappingException;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.sequence.spi.SequenceSupport;

/**
 * Sequence support for {@link TiDBDialect}.
 *
 * @author Cong Wang
 */
public class TiDBSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new TiDBSequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "nextval(" + sequenceName + ")";
	}

	@Override
	public String getSelectSequencePreviousValString(String sequenceName) throws MappingException {
		return "lastval(" + sequenceName + ")";
	}

	@Override
	public @Nullable String getSequenceNextValParameterizedString(String sequenceName) throws MappingException {
		return "with recursive cte as (select 1 v union all select t.v + 1 from cte t where t.v<?) select "
			+ getSelectSequenceNextValString( sequenceName ) + " from cte t";
	}

	@Override
	public int maxSequenceBatchSize() {
		// the recursive CTE is limited by cte_max_recursion_depth (default 1000)
		return 1000;
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		return getCreateSequenceString( sequenceName )
				+ " start with " + initialValue
				+ " increment by " + incrementSize
				+ startingValue( initialValue, incrementSize );
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

}
