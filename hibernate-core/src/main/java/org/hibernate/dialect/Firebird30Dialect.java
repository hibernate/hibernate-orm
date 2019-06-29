/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.MappingException;
import org.hibernate.dialect.identity.Firebird30IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQL2008StandardLimitHandler;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorFirebird30DatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * Dialect for Firebird 3.0.
 * <p>
 * Adds support for
 * <ul>
 *     <li>pooled sequences</li>
 *     <li>SQL standard offset/fetch</li>
 *     <li>identity columns</li>
 * </ul>
 * </p>
 *
 * @author Mark Rotteveel
 */
public class Firebird30Dialect extends Firebird25Dialect {

	public Firebird30Dialect() {
		super();
		registerColumnType( Types.BOOLEAN, "boolean" );
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		return new String[] { getCreateSequenceString( sequenceName, initialValue, incrementSize ) };
	}

	@Override
	protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize)
			throws MappingException {
		// NOTE currently has an 'off by increment' bug, see http://tracker.firebirdsql.org/browse/CORE-6084
		if (initialValue == 1 && incrementSize == 1) {
			// Workaround for 'off by increment' bug for initial value and increment 1
			return getCreateSequenceString( sequenceName );
		}
		return getCreateSequenceString( sequenceName ) + " start with " + initialValue + " increment by " + incrementSize;
	}

	@Override
	public String getQuerySequencesString() {
		// NOTE currently has an 'off by increment' bug, see http://tracker.firebirdsql.org/browse/CORE-6084
		// May need revision depending on the final solution (eg second column might need to be changed to RDB$INITIAL_VALUE + RDB$GENERATOR_INCREMENT)
		return "select RDB$GENERATOR_NAME, RDB$INITIAL_VALUE, RDB$GENERATOR_INCREMENT from RDB$GENERATORS where coalesce(RDB$SYSTEM_FLAG, 0) = 0";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorFirebird30DatabaseImpl.INSTANCE;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return SQL2008StandardLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new Firebird30IdentityColumnSupport();
	}

	@Override
	public boolean supportsExistsInSelect() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return bool ? "true" : "false";
	}
}
