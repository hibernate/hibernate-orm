/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.ant;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.types.Path;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategyFactory;


/**
 * @author max
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class JDBCConfigurationTask extends ConfigurationTask {

	boolean preferBasicCompositeIds = true;
    
	String reverseEngineeringStrategyClass;
	String packageName;
	Path revengFiles;

	boolean detectOneToOne = true;
	boolean detectManyToMany = true;
	boolean detectOptimisticLock = true;
    
	public JDBCConfigurationTask() {
		setDescription("JDBC Configuration (for reverse engineering)");
	}
	protected MetadataDescriptor createMetadataDescriptor() {
		Properties properties = loadProperties();
		RevengStrategy res = createReverseEngineeringStrategy();
		properties.put(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS, preferBasicCompositeIds);
		return MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(
						res, 
						properties);
	}
	
	private RevengStrategy createReverseEngineeringStrategy() {
		File[] revengFileList = null;
		if (revengFiles != null ) {
			String[] fileNames = revengFiles.list();
			revengFileList = new File[fileNames.length];
			for (int i = 0; i < fileNames.length; i++) {
				revengFileList[i] = new File(fileNames[i]);
			}
		}

		RevengStrategy strategy = 
				RevengStrategyFactory.createReverseEngineeringStrategy(
                        reverseEngineeringStrategyClass, revengFileList);

		RevengSettings qqsettings = 
			new RevengSettings(strategy).setDefaultPackageName(packageName)
			.setDetectManyToMany( detectManyToMany )
			.setDetectOneToOne( detectOneToOne )
			.setDetectOptimisticLock( detectOptimisticLock );
	
		strategy.setSettings(qqsettings);
		
		return strategy;
	}

    
    public void setPackageName(String pkgName) {
        packageName = pkgName;
    }
    
    public void setReverseStrategy(String fqn) {
        reverseEngineeringStrategyClass = fqn;
    }
    
	public void setRevEngFile(Path p) {
		revengFiles = p;		
	}
	
	public void setPreferBasicCompositeIds(boolean b) {
		preferBasicCompositeIds = b;
	}
	
	public void setDetectOneToOne(boolean b) {
		detectOneToOne = b;
	}
	
	public void setDetectManyToMany(boolean b) {
		detectManyToMany = b;
	}
	
	public void setDetectOptimisticLock(boolean b) {
		detectOptimisticLock = b;
	}

	private Map<String, Object> loadCfgXmlFile() {
		return new ConfigLoader(new BootstrapServiceRegistryBuilder().build())
				.loadConfigXmlFile(getConfigurationFile())
				.getConfigurationValues();
	}

	private Properties loadProperties() {
		Properties result = new Properties();
		if (getPropertyFile() != null) {
			result.putAll(loadPropertiesFile());
		}
		if (getConfigurationFile() != null) {
			result.putAll(loadCfgXmlFile());
		}
		return result;
	}

	public Object clone() throws CloneNotSupportedException {
		JDBCConfigurationTask jct = (JDBCConfigurationTask) super.clone();
		jct.preferBasicCompositeIds = this.preferBasicCompositeIds;
		jct.reverseEngineeringStrategyClass = this.reverseEngineeringStrategyClass;
		jct.packageName = this.packageName;
		jct.revengFiles = this.revengFiles;
		jct.detectOneToOne = this.detectOneToOne;
		jct.detectManyToMany = this.detectManyToMany;
		jct.detectOptimisticLock = this.detectOptimisticLock;
		return jct;
	}
}
