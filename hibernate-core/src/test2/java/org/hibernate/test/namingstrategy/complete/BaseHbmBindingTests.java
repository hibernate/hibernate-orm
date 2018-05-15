/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.complete;

import org.hibernate.boot.MetadataSources;

/**
 * @author Steve Ebersole
 */
public abstract class BaseHbmBindingTests extends BaseNamingTests {
	@Override
	protected void applySources(MetadataSources metadataSources) {
		metadataSources.addResource( "org/hibernate/test/namingstrategy/complete/Mappings.hbm.xml" );
	}
}
