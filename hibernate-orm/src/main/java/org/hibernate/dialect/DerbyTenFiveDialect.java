/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.AnsiTrimFunction;
import org.hibernate.dialect.function.DerbyConcatFunction;


/**
 * Hibernate Dialect for Cloudscape 10 - aka Derby. This implements both an
 * override for the identity column generator as well as for the case statement
 * issue documented at:
 * http://www.jroller.com/comments/kenlars99/Weblog/cloudscape_soon_to_be_derby
 *
 * @author Simon Johnston
 * @author Scott Marlow
 */
@SuppressWarnings("deprecation")
public class DerbyTenFiveDialect extends DerbyDialect {
	/**
	 * Constructs a DerbyTenFiveDialect
	 */
	public DerbyTenFiveDialect() {
		super();
		registerFunction( "concat", new DerbyConcatFunction() );
		registerFunction( "trim", new AnsiTrimFunction() );
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}
}
