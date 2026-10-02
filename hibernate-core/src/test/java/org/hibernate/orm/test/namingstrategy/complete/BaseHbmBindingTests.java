package org.hibernate.orm.test.namingstrategy.complete;

import org.hibernate.boot.MetadataSources;

/**
 * @author Steve Ebersole
 */
public abstract class BaseHbmBindingTests extends BaseNamingTests {
	@Override
	protected void applySources(MetadataSources metadataSources) {
		metadataSources.addResource( "org/hibernate/orm/test/namingstrategy/complete/Mappings.hbm.xml" );
	}
}
