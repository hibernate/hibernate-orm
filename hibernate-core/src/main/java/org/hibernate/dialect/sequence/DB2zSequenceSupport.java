/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.DB2zDialect}.
 *
 * @author Gavin King
 */
public final class DB2zSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new DB2zSequenceSupport();

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
