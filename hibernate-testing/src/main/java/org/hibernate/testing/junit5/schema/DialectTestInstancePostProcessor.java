/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.schema;


import org.hibernate.testing.junit5.DialectAccess;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

/**
 * @author Andrea Boriero
 */
public class DialectTestInstancePostProcessor implements TestInstancePostProcessor {
	@Override
	public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
		if ( DialectAccess.class.isInstance( testInstance ) ) {
			context.getStore( DialectAccess.NAMESPACE ).put( testInstance, testInstance );
		}
	}
}
