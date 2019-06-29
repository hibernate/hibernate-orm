/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.sequence;

/**
 * Sequence support for {@link org.hibernate.dialect.CacheDialect}.
 *
 * Use of sequences on Cache is not recommended.
 *
 * @author Gavin King
 */
public final class CacheSequenceSupport implements SequenceSupport {

	public static final SequenceSupport INSTANCE = new CacheSequenceSupport();

	public String getSequenceNextValString(String sequenceName) {
		return "select InterSystems.Sequences_GetNext('" + sequenceName + "')" + getFromDual( sequenceName );
	}

	public String getSelectSequenceNextValString(String sequenceName) {
		//TODO: is this really correct? Why can't we just call InterSystems.Sequences_GetNext() without the select?
		return "(select InterSystems.Sequences_GetNext('" + sequenceName + "')" + getFromDual( sequenceName ) + ")";
	}

	private String getFromDual(String sequenceName) {
		return " from InterSystems.Sequences where ucase(name)=ucase('" + sequenceName + "')";
	}

	public String getCreateSequenceString(String sequenceName) {
		return "insert into InterSystems.Sequences(Name) values (ucase('" + sequenceName + "'))";
	}

	public String getDropSequenceString(String sequenceName) {
		return "delete from InterSystems.Sequences where ucase(name)=ucase('" + sequenceName + "')";
	}

	@Override
	public boolean supportsPooledSequences() {
		return false;
	}
}
