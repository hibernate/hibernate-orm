/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface RuntimeModelCreationContext extends PersisterCreationContext {
	SessionFactoryImplementor getSessionFactory();

	BootstrapContext getBootstrapContext();

	default TypeConfiguration getTypeConfiguration() {
		return getBootstrapContext().getTypeConfiguration();
	}

	MetadataImplementor getMetadata();

	default ManagedBeanRegistry getManagedBeanRegistry() {
		return getSessionFactory().getServiceRegistry().getService( ManagedBeanRegistry.class );
	}

	default JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
		return getTypeConfiguration().getJavaTypeDescriptorRegistry();
	}
}
