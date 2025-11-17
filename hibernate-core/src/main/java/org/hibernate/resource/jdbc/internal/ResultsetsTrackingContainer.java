/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.internal;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * We used to record Statement(s) and their associated ResultSet(s) in a Map
 * to guarantee proper resource cleanup, however such types will commonly
 * be implemented in such a way to require identity hashcode calculations, which has
 * been shown to become a drag on overall system efficiency
 * (There are JVM tunables one can use to improve on the default, but they have
 * system wide impact which in turn could have undesirable impact on other libraries).
 * Since in the most common case we process a single statement at a time, we
 * trade some code complexity here to keep track of such resources via direct
 * fields only, and overflow to the normal Map usage for the remaining cases, which
 * are typically less frequent.
 */
final class ResultsetsTrackingContainer {

	//Implementation notes:
	// #1. if key_1 is non-null, then value_1 is the value it maps to.
	// #2. if key_1 is null, then the Map in xref is guaranteed to be empty
	// #3. The Map in xref is lazily initialized, but when emptied it's not guaranteed to be made null

	private static final ResultSetsSet EMPTY = new ResultSetsSet();

	private Statement key_1;
	private ResultSetsSet value_1;

	//Additional pairs, for the case in which we need more:
	private HashMap<Statement, ResultSetsSet> xref;

	public boolean hasRegisteredResources() {
		return key_1 != null; //No need to check the content of xref because of implementation rule #1.
	}

	public void registerExpectingNew(final Statement statement) {
		//We use an assert here as it's a relatively expensive check and I'm fairly confident this would never happen at runtime,
		//yet we keep the check as an assertion to have the testsuite help us ensure this confidence is maintained in the future.
		assert statementNotExisting( statement ) : "JDBC Statement already registered";
		if ( key_1 == null ) {
			//this is the fast-path: most likely case and most efficient as we avoid accessing xref altogether
			key_1 = statement;
			value_1 = EMPTY;
		}
		else {
			getXrefForWriting().put( statement, EMPTY );
		}
	}

	//Assertion helper only:
	private boolean statementNotExisting(final Statement statement) {
		if ( key_1 == statement ) {
			return false;
		}
		else if ( xref != null ) {
			return ! xref.containsKey( statement );
		}
		else {
			return true;
		}
	}

	private HashMap<Statement, ResultSetsSet> getXrefForWriting() {
		if ( this.xref == null ) {
			this.xref = new HashMap<>();
		}
		return this.xref;
	}

	private void trickleDown() {
		//Moves the first entry from the xref map into the fields, if any entry exists in it.
		if ( xref != null ) {
			Iterator<Map.Entry<Statement, ResultSetsSet>> iterator = xref.entrySet().iterator(); {
				if ( iterator.hasNext() ) {
					Map.Entry<Statement, ResultSetsSet> entry = iterator.next();
					key_1 = entry.getKey();
					value_1 = entry.getValue();
					iterator.remove();
				}
			}
		}
	}

	public void storeAssociatedResultset(Statement statement, ResultSet resultSet) {
		if ( key_1 == null ) {
			key_1 = statement;
			value_1 = new ResultSetsSet();
			//A debug warning wrapped in an assertion to avoid its overhead in production systems
			assert warnOnNotNull( null );
			value_1.storeResultSet( resultSet );
		}
		else if ( key_1 == statement ) {
			value_1 = ensureWriteable( value_1 );
			value_1.storeResultSet( resultSet );
		}
		else {
			ResultSetsSet resultSetsSet = null;
			if ( xref == null ) {
				xref = new HashMap<>();
			}
			else {
				resultSetsSet = xref.get( statement );
			}
			//A debug warning wrapped in an assertion to avoid its overhead in production systems
			assert warnOnNotNull( resultSetsSet );
			if ( resultSetsSet == null || resultSetsSet == EMPTY ) {
				resultSetsSet = new ResultSetsSet();
				xref.put( statement, resultSetsSet );
			}
			resultSetsSet.storeResultSet( resultSet );
		}
	}

	private ResultSetsSet ensureWriteable(final ResultSetsSet value) {
		if ( value == null || value == EMPTY) {
			return new ResultSetsSet();
		}
		else {
			return value;
		}
	}

	//As it's a get "for removal" we won't be wrapping an EMPTY set

	/**
	 * This gets the {@link ResultSetsSet} associated to a particular statement,
	 * but should only be used for read operations or to remove resultsets.
	 * Performing an "add" operation could result in tainting constants.
	 * This is NOT removing the statement - use {@link #remove(Statement)} for that purpose.
	 * @param statement
	 * @return
	 */
	public ResultSetsSet getForResultSetRemoval(final Statement statement) {
		final ResultSetsSet existingEntry;
		if ( key_1 == statement ) {
			existingEntry = value_1;
		}
		else if ( key_1 != null && xref != null ) {
			existingEntry = xref.get( statement );
		}
		else {
			existingEntry = null;
		}

		//A debug warning wrapped in an assertion to avoid its overhead in production systems
		assert warnOnNotNull( existingEntry );

		return existingEntry;
	}

	public ResultSetsSet remove(final Statement statement) {
		if ( key_1 == statement ) {
			final ResultSetsSet v = value_1;
			key_1 = null;
			value_1 = null;
			trickleDown(); //most expensive operation, but necessary to guarantee the invariants which allow the other optimisations
			return v;
		}
		else if ( xref != null ) {
			return xref.remove( statement );
		}
		return null;
	}

	private boolean warnOnNotNull(ResultSetsSet existingEntry) {
		// Keep this at DEBUG level, rather than WARN.
		// Connection pool implementations often return a
		// proxy/wrapper around the JDBC Statement,
		// causing excessive logging here. See HHH-8210.
		if ( existingEntry == null ) {
			CORE_LOGGER.trace( "ResultSet statement was not registered (on register)" );
		}
		return true;
	}

	public void forEach(final BiConsumer<Statement, ResultSetsSet> action) {
		if ( key_1 != null ) {
			action.accept( key_1, value_1 );
			if ( xref != null ) {
				xref.forEach( action );
			}
		}
	}

	public void clear() {
		key_1 = null;
		value_1 = null;
		if ( xref != null ) {
			xref.clear();
			xref = null;
		}
	}

}
