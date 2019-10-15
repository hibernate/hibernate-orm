/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4.runtimecheck;

import org.jboss.byteman.rule.helper.Helper;

import org.jboss.byteman.rule.Rule;

/**
 * Base {@link Helper} defining common methods to use in the Byteman rules
 * to check whether some forbidden APIs are invoked at runtime.
 *
 * @see BMRuntimeCheckCustomRunner
 * @author Fabio Massimo Ercoli
 */
public class BMRuntimeCheckHelper extends Helper {

	private static final String SESSION_FACTORY_INIT_FLAG = "session-factory-init";
	private static final int FRAME_SIZE_CHECK = 100;

	protected BMRuntimeCheckHelper(Rule rule) {
		super( rule );
	}

	public void markSessionFactoryInCreation() {
		flag( SESSION_FACTORY_INIT_FLAG );
	}

	public void markSessionFactoryCreated() {
		clear( SESSION_FACTORY_INIT_FLAG );
	}

	/**
	 * Api check is supposed to be done when:
	 * 1. Session factory has already been created and it is active.
	 * 2. There is no class initialization in the frame set of the calling methods
	 * 		{@code E.g. <code>static final Pattern PATTERN = Pattern.compile( "aaa" );</code>; should be legal}.
	 *
	 * @return whether the API should be checked
	 */
	public boolean shouldPerformAPICheck() {
		return shouldPerformAPICheck( false );
	}

	/**
	 * Api check is supposed to be done when:
	 * 1. Session factory has already been created and it is active.
	 * 2. There is no class initialization in the frame set of the calling methods
	 * 		{@code E.g. <code>static final Pattern PATTERN = Pattern.compile( "aaa" );</code>; should be legal}.
	 * 3. If {@code skipJUnitFrameworkMethod} is true,
	 * 		there is no {@link org.junit.runners.model.FrameworkMethod} in the last 2 calls of the frame set.
	 *
	 * @param skipJUnitFrameworkMethod whether to skip the check if it has been called by {@link org.junit.runners.model.FrameworkMethod}
	 * @return whether the API should be checked
	 */
	public boolean shouldPerformAPICheck(boolean skipJUnitFrameworkMethod) {
		// setTriggering( false ) allows to not trigger the rule from the rule itself
		// FRAME_SIZE_CHECK as frameCount should be enough to filter any call coming from any class initialization
		boolean outcome = !flagged( SESSION_FACTORY_INIT_FLAG ) && setTriggering( false )
				&& !callerMatches( "<clinit>", false, FRAME_SIZE_CHECK );

		if ( skipJUnitFrameworkMethod ) {
			outcome &= !callerMatches( "org.junit.runners.model.FrameworkMethod.*", true, true, 2 );
		}

		return outcome;
	}
}
