/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.MappingException;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class LegacySequenceSupport implements SequenceSupport {
	private Dialect dialect;

	public LegacySequenceSupport(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public boolean supportsSequences() {
		return dialect.supportsSequences();
	}

	@Override
	public boolean supportsPooledSequences() {
		return dialect.supportsPooledSequences();
	}

	@Override
	public String getSequenceNextValString(String sequenceName) throws MappingException {
		return dialect.getSequenceNextValString( sequenceName );
	}

	@Override
	public String getSequenceNextValString(String sequenceName, int increment) throws MappingException {
		return dialect.getSequenceNextValString( sequenceName, increment );
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
		return dialect.getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		return dialect.getCreateSequenceStrings( sequenceName, initialValue, incrementSize );
	}

	@Override
	public String getCreateSequenceString(String sequenceName) throws MappingException {
		return dialect.getCreateSequenceString( sequenceName );
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		return dialect.getCreateSequenceString( sequenceName, initialValue, incrementSize );
	}

	@Override
	public String[] getDropSequenceStrings(String sequenceName) throws MappingException {
		return dialect.getDropSequenceStrings( sequenceName );
	}

	@Override
	public String getDropSequenceString(String sequenceName) throws MappingException {
		return dialect.getDropSequenceString( sequenceName );
	}
}
