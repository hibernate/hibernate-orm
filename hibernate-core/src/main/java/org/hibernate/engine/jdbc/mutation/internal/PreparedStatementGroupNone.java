/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;

/**
 * @author Steve Ebersole
 */
public class PreparedStatementGroupNone implements PreparedStatementGroup {
	/**
	 * Singleton access
	 */
	public static final PreparedStatementGroupNone GROUP_OF_NONE = new PreparedStatementGroupNone();

	@Override
	public int getNumberOfStatements() {
		return 0;
	}

	@Override
	public int getNumberOfActiveStatements() {
		return 0;
	}

	@Override
	public PreparedStatementDetails getSingleStatementDetails() {
		return null;
	}

	@Override
	public void forEachStatement(BiConsumer<String, PreparedStatementDetails> action) {
	}

	@Override
	public PreparedStatementDetails resolvePreparedStatementDetails(String tableName) {
		return null;
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		return null;
	}

	@Override
	public boolean hasMatching(Predicate<PreparedStatementDetails> filter) {
		return false;
	}

	@Override
	public void release() {
	}
}
