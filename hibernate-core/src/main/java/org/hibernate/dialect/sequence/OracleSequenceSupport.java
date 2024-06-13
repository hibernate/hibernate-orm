/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

/**
 * Sequence support for {@link org.hibernate.dialect.OracleDialect}.
 *
 * @author Gavin King
 * @author Loïc Lefèvre
 *
 * @see <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-SEQUENCE.html">Oracle Database Documentation</a>
 */
public final class OracleSequenceSupport extends NextvalSequenceSupport {
	/**
	 * @deprecated Construct instance based on version instead.
	 */
	@Deprecated(forRemoval = true)
	public static final SequenceSupport INSTANCE = new OracleSequenceSupport( true, false );

	public static SequenceSupport getInstance(final Dialect dialect) {
		return new OracleSequenceSupport(dialect.getVersion());
	}

	private final boolean requiresFromDual;
	private final boolean supportsIfExists;

	// TODO: HHH-18144 - Support Oracle sequence optional KEEP for Application Continuity
	// TODO: HHH-18143 - Support Oracle scalable sequences for RAC

	public OracleSequenceSupport(final DatabaseVersion version) {
		this( version.isBefore( 23 ), version.isSameOrAfter( 23 ) );
	}

	OracleSequenceSupport(boolean requiresFromDual, boolean supportsIfExists) {
		this.requiresFromDual = requiresFromDual;
		this.supportsIfExists = supportsIfExists;
	}

	@Override
	public String getFromDual() {
		return requiresFromDual ? " from dual" : "";
	}

	@Override
	public boolean sometimesNeedsStartingValue() {
		return true;
	}

	@Override
	public String getDropSequenceString(String sequenceName) throws MappingException {
		return "drop sequence " + (supportsIfExists ? "if exists " : "") + sequenceName;
	}
}
