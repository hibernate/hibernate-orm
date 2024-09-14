/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.assertj;

import org.hibernate.Hibernate;

import org.assertj.core.api.Condition;

/**
 * @author Steve Ebersole
 */
public class HibernateInitializedCondition extends Condition<Object> {
	public static final HibernateInitializedCondition IS_INITIALIZED = new HibernateInitializedCondition( true );
	public static final HibernateInitializedCondition IS_NOT_INITIALIZED = new HibernateInitializedCondition( false );

	private final boolean positive;

	public HibernateInitializedCondition(boolean positive) {
		super( "Hibernate#isInitialized check" );
		this.positive = positive;
	}

	@Override
	public boolean matches(Object value) {
		return positive == Hibernate.isInitialized( value );
	}
}
