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
public abstract class BaseAnnotationBindingTests extends BaseNamingTests {
	@Override
	protected void applySources(MetadataSources metadataSources) {
		metadataSources.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Customer.class )
				.addAnnotatedClass( Industry.class )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( ZipCode.class );
	}
}
