/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;


/**
 * @author Steve Ebersole
 */
public class LongHolderTest extends AbstractHolderTest {
	protected IntegralDataTypeHolder makeHolder() {
		return IdentifierGeneratorHelper.getIntegralDataTypeHolder( Long.class );
	}
}
