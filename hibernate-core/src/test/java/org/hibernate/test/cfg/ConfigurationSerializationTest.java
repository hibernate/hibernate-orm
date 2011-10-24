/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.cfg;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * Copied over mostly from ConfigurationPerformanceTest
 *
 * @author Steve Ebersole
 * @author Max Andersen
 */
public class ConfigurationSerializationTest extends BaseUnitTestCase {
	private static final String[] FILES = new String[] {
			"legacy/ABC.hbm.xml",
			"legacy/ABCExtends.hbm.xml",
			"legacy/Baz.hbm.xml",
			"legacy/Blobber.hbm.xml",
			"legacy/Broken.hbm.xml",
			"legacy/Category.hbm.xml",
			"legacy/Circular.hbm.xml",
			"legacy/Commento.hbm.xml",
			"legacy/ComponentNotNullMaster.hbm.xml",
			"legacy/Componentizable.hbm.xml",
			"legacy/Container.hbm.xml",
			"legacy/Custom.hbm.xml",
			"legacy/CustomSQL.hbm.xml",
			"legacy/Eye.hbm.xml",
			"legacy/Fee.hbm.xml",
			"legacy/Fo.hbm.xml",
			"legacy/FooBar.hbm.xml",
			"legacy/Fum.hbm.xml",
			"legacy/Fumm.hbm.xml",
			"legacy/Glarch.hbm.xml",
			"legacy/Holder.hbm.xml",
			"legacy/IJ2.hbm.xml",
			"legacy/Immutable.hbm.xml",
			"legacy/Location.hbm.xml",
			"legacy/Many.hbm.xml",
			"legacy/Map.hbm.xml",
			"legacy/Marelo.hbm.xml",
			"legacy/MasterDetail.hbm.xml",
			"legacy/Middle.hbm.xml",
			"legacy/Multi.hbm.xml",
			"legacy/MultiExtends.hbm.xml",
			"legacy/Nameable.hbm.xml",
			"legacy/One.hbm.xml",
			"legacy/ParentChild.hbm.xml",
			"legacy/Qux.hbm.xml",
			"legacy/Simple.hbm.xml",
			"legacy/SingleSeveral.hbm.xml",
			"legacy/Stuff.hbm.xml",
			"legacy/UpDown.hbm.xml",
			"legacy/Vetoer.hbm.xml",
			"legacy/WZ.hbm.xml",
			"cfg/orm-serializable.xml"
	};

	@Test
	public void testConfigurationSerializability() {
		Configuration cfg = new Configuration();
		for ( String file : FILES ) {
			cfg.addResource( "org/hibernate/test/" + file );
		}

		cfg.addAnnotatedClass( Serial.class );

		byte[] bytes = SerializationHelper.serialize( cfg );
		cfg = ( Configuration ) SerializationHelper.deserialize( bytes );

		SessionFactory factory = null;
		ServiceRegistry serviceRegistry = null;
		try {
			serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
			// try to build SF
			factory = cfg.buildSessionFactory( serviceRegistry );
		}
		finally {
			if ( factory != null ) {
				factory.close();
			}
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}
	}

	@Entity
	public static class Serial {
		private String id;
		private String value;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
