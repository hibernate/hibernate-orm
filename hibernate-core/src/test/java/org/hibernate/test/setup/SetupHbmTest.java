package org.hibernate.test.setup;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

public class SetupHbmTest {

	@Test
	public void checkTableConfig() {
		Map<Object, Object> properties = new HashMap<Object, Object>();
		properties.put(AvailableSettings.DIALECT, org.hibernate.dialect.HSQLDialect.class.getCanonicalName());
		StandardServiceRegistryBuilder builder =  new StandardServiceRegistryBuilder().applySettings( properties);
		StandardServiceRegistry standardregistry = builder.build();
		MetadataSources metadataSources = new MetadataSources(standardregistry);
		
		metadataSources.addResource("org/hibernate/test/setup/Car.hbm.xml");
		MetadataBuilder metadataBuilder=metadataSources.getMetadataBuilder();
		MetadataImplementor metadata = (MetadataImplementor) metadataBuilder.build();
	}
}
