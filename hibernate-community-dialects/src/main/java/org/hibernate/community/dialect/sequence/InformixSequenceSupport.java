/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.sequence;

import org.hibernate.dialect.sequence.NextvalSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;

/**
 * Sequence support for {@link org.hibernate.community.dialect.InformixDialect}.
 *
 * @author Gavin King
 */
public final class InformixSequenceSupport extends NextvalSequenceSupport {

	public static final SequenceSupport INSTANCE = new InformixSequenceSupport( false );

	private final boolean supportsIfExists;

	public InformixSequenceSupport(boolean supportsIfExists){
		this.supportsIfExists = supportsIfExists;
	}

	@Override
	public String getFromDual() {
		return " from informix.systables where tabid=1";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + ( supportsIfExists ? "if exists " : "" ) + sequenceName;
	}
}
