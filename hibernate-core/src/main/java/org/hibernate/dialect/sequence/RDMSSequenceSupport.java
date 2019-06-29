/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

import org.hibernate.MappingException;

/**
 * Sequence support for {@link org.hibernate.dialect.RDMSOS2200Dialect}.
 *
 * Note that RDMS doesn't really have sequences as such, but it does
 * have the GUID-like {@code permuted_id()} and {@code unique_id()}
 * functions which generate unique integers.
 *
 * @author Gavin King
 */
public final class RDMSSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new RDMSSequenceSupport();

	@Override
	public String getSelectSequenceNextValString(String sequenceName) throws MappingException {
		return "permuted_id('NEXT',31)";
	}

	@Override
	public String getFromDual() {
		// The where clause was added to eliminate this statement from Brute Force Searches.
		return " from rdms.rdms_dummy where key_col = 1";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		// We must return a valid RDMS/RSA command from this method to
		// prevent RDMS/RSA from issuing *ERROR 400
		return "";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		// We must return a valid RDMS/RSA command from this method to
		// prevent RDMS/RSA from issuing *ERROR 400
		return "";
	}

	@Override
	public boolean supportsPooledSequences() {
		return false;
	}
}
