/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.HSQLDialect}.
 *
 * @author Gavin King
 */
public final class HSQLSequenceSupport extends ANSISequenceSupport {

	public static final SequenceSupport INSTANCE = new HSQLSequenceSupport();

	/**
	 * HSQL will start with 0, by default.  In order for Hibernate to know that this not transient,
	 * manually start with 1.
	 */
	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName + " start with 1";
	}

	/**
	 * Because of the overridden {@link #getCreateSequenceString(String)}, we must also override
	 * {@link org.hibernate.dialect.Dialect#getCreateSequenceString(String, int, int)} to prevent
	 * duplication of {@code start with}.
	 */
	@Override
	public String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
		return "create sequence " + sequenceName + " start with " + initialValue + " increment by " + incrementSize;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " +  sequenceName + " if exists";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "call " + getSelectSequenceNextValString( sequenceName );
	}
}
