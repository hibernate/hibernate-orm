/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;


/**
 * An SQL dialect for DB2/390 version 8.
 *
 * @author Tobias Sternvik
 */
public class DB2390V8Dialect extends DB2390Dialect {

	@Override
	public boolean supportsSequences() {
		return true;
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select nextval for " + sequenceName + " from sysibm.sysdummy1";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName + " as integer start with 1 increment by 1 minvalue 1 nomaxvalue nocycle nocache"; //simple default settings..
	}

	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName +  " restrict";
	}

	public String getQuerySequencesString() {
		return "select * from sysibm.syssequences";
	}
}
