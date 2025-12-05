/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Similar goal as in {@link ResultsetsTrackingContainer}: make sure to
 * be very efficient when handling a single {@link ResultSet}
 * as it's a very common case, and try to avoid needing the hashcodes.
 * Yet we want to allow scaling to multiple instances as well.
 */
final class ResultSetsSet {

	//Implementation notes:
	// # if first is null, then the Map in field 'more' is guaranteed to be empty
	// # The 'more' Map is lazily initialized, but when emptied it's not guaranteed to be made null

	private ResultSet first;
	//This could have been a set, but we intentionally use a Map instead to avoid the wrapping done in
	//HashSet.
	private HashMap<ResultSet,ResultSet> more;

	void forEachResultSet(final Consumer<ResultSet> action) {
		if ( first != null ) {
			action.accept( first );
			if ( more != null ) {
				more.keySet().forEach( action );
			}
		}
	}

	void storeResultSet(final ResultSet resultSet) {
		if ( first == null ) {
			//no need for further checks as we guarantee "more" to be empty in this case
			first = resultSet;
		}
		else if ( first == resultSet ) {
			//no-op for this special case
		}
		else {
			if ( more == null ) {
				more = new HashMap<>();
			}
			more.put( resultSet, resultSet );
		}
	}

	boolean isEmpty() {
		return first == null;
	}

	ResultSet removeResultSet(final ResultSet resultSet) {
		if ( first == resultSet ) {
			ResultSet v = first;
			first = null;
			scaleDown();
			return v;
		}
		else if ( more != null ) {
			return more.remove( resultSet );
		}
		else {
			return null;
		}
	}

	//When slot "first" is made available, make sure to move an entry from "more" into that field.
	//Any entry will do, so we take the first one if there's any.
	private void scaleDown() {
		if ( more != null && !more.isEmpty() ) {
			var iterator = more.entrySet().iterator();
			var entry = iterator.next();
			final var resultSet = entry.getKey();
			iterator.remove();
			first = resultSet;
		}
	}

	void clear() {
		first = null;
		if ( more != null ) {
			more.clear();
			this.more = null;
		}
	}

}
