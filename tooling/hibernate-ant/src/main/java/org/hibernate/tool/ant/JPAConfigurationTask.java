/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2004-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ant;

import java.io.File;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.hibernate.HibernateException;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;

public class JPAConfigurationTask extends ConfigurationTask {
	
	private String persistenceUnit = null;

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
			} else {
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
		complain("configurationfile");
	}

	private void complain(String param) {
		throw new BuildException("<" + getTaskName() + "> currently only support autodiscovery from META-INF/persistence.xml. Thus setting the " + param + " attribute is not allowed");
	}
	
	
}
