/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.hibernate.HibernateException;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;

import java.io.File;
import java.util.Properties;

public class JPAConfigurationTask extends ConfigurationTask {

	String persistenceUnit = null;

	public JPAConfigurationTask() {
		setDescription("JPA Configuration");
	}

	protected MetadataDescriptor createMetadataDescriptor() {
		try {
			Properties overrides = new Properties();
			Properties p = loadPropertiesFile();
			if(p!=null) {
				overrides.putAll( p );
			}
			return MetadataDescriptorFactory
					.createJpaDescriptor(persistenceUnit, overrides);
		}
		catch(HibernateException t) {
			Throwable cause = t.getCause();
			if (cause != null) {
				throw new BuildException(cause);
			}
			else {
				throw new BuildException("Problems in creating a configuration for JPA. Have you remembered to add hibernate EntityManager jars to the classpath ?",t);
			}
		}

	}

	public String getPersistenceUnit() {
		return persistenceUnit;
	}

	public void setPersistenceUnit(String persistenceUnit) {
		this.persistenceUnit = persistenceUnit;
	}

	public void setConfigurationFile(File configurationFile) {
		complain();
	}

	private void complain() {
		throw new BuildException("<" + getTaskName() + "> currently only support autodiscovery from META-INF/persistence.xml. Thus setting the configurationfile attribute is not allowed");
	}

	public Object clone() throws CloneNotSupportedException {
		JPAConfigurationTask jct = (JPAConfigurationTask) super.clone();
		jct.persistenceUnit = this.persistenceUnit;
		return jct;
	}


}
