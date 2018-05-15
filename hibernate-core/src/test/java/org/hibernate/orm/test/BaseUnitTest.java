/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test;

import org.hibernate.testing.junit5.ExpectedExceptionExtension;
import org.hibernate.testing.junit5.FailureExpectedExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@ExtendWith( FailureExpectedExtension.class )
@ExtendWith( ExpectedExceptionExtension.class )
public abstract class BaseUnitTest {

	@SuppressWarnings("unchecked")
	protected  <T> T cast(Object thing, Class<T> type) {
		assertThat( thing, instanceOf( type ) );
		return type.cast( thing );
	}
}
