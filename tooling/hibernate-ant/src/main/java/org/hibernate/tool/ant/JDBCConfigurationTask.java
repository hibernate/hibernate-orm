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
import java.lang.reflect.Constructor;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.RevengStrategyFactory;
import org.hibernate.tool.util.ReflectionUtil;


/**
 * @author max
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class JDBCConfigurationTask extends ConfigurationTask {
	//not expfosed here.
    private boolean preferBasicCompositeIds = true;
    
    private String reverseEngineeringStrategyClass;
    private String packageName;
	private Path revengFiles;

	private boolean detectOneToOne = true;
	private boolean detectManyToMany = true;
	private boolean detectOptimisticLock = true;
    
	public JDBCConfigurationTask() {
		setDescription("JDBC Configuration (for reverse engineering)");
	}
	protected MetadataDescriptor createMetadataDescriptor() {
		Properties properties = loadPropertiesFile();
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
						null, revengFileList);
		
		if(reverseEngineeringStrategyClass!=null) {
			strategy = loadreverseEngineeringStrategy(reverseEngineeringStrategyClass, strategy);			
		}
		
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
	
    private RevengStrategy loadreverseEngineeringStrategy(final String className, RevengStrategy delegate) 
    throws BuildException {
        try {
            Class<?> clazz = ReflectionUtil.classForName(className);			
			Constructor<?> constructor = clazz.getConstructor(new Class[] { RevengStrategy.class });
            return (RevengStrategy) constructor.newInstance(new Object[] { delegate }); 
        } 
        catch (NoSuchMethodException e) {
			try {
				getProject().log("Could not find public " + className + "(ReverseEngineeringStrategy delegate) constructor on ReverseEngineeringStrategy. Trying no-arg version.",Project.MSG_VERBOSE);			
				Class<?> clazz = ReflectionUtil.classForName(className);	
				Constructor<?> constructor = clazz.getConstructor(new Class[] {});
				RevengStrategy rev = (RevengStrategy) constructor.newInstance();
				getProject().log("Using non-delegating strategy, thus packagename and revengfile will be ignored.", Project.MSG_INFO);
				return rev;
			} 
			catch (Exception eq) {
				throw new BuildException("Could not create or find " + className + " with default no-arg constructor", eq);
			}
		} 
        catch (Exception e) {
			throw new BuildException("Could not create or find " + className + " with one argument delegate constructor", e);
		} 
    }
}
