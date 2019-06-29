/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * An instance of {@link SequenceSupport} support indicating that
 * the SQL dialect does not support sequences.
 *
 * @author Gavin King
 */
public class NoSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new NoSequenceSupport();

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public boolean supportsPooledSequences() {
		return false;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}

	@Override
	public String getSequenceNextValString(String sequenceName, int increment) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}

	@Override
	public String[] getCreateSequenceStrings(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}

	@Override
	public String getCreateSequenceString(String sequenceName) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}

	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}

	@Override
	public String[] getDropSequenceStrings(String sequenceName) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}

	@Override
	public String getDropSequenceString(String sequenceName) throws MappingException {
		throw new MappingException("dialect does not support sequences");
	}
}
