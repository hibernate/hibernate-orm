/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.export.hbm;

import org.hibernate.internal.util.StringHelper;

/**
 * This class replicates the global settings that can be selected
 * within the mapping document. This is provided to allow a GUI
 * too to choose these settings and thus the generated mapping
 * document will include them.
 * 
 * @author David Channon
 */
public class HibernateMappingGlobalSettings {

	private String schemaName;
    private String catalogName;
	private String defaultCascade;
	private String defaultPackage;
	private String defaultAccess;
	private boolean autoImport  = true;
	private boolean defaultLazy = true;
	
	/**
	 */
	public HibernateMappingGlobalSettings() {
	}
	
	public boolean hasNonDefaultSettings() {
		return 	 this.hasDefaultPackage()	||
				 this.hasSchemaName() 		||
				 this.hasCatalogName()		||
				 this.hasNonDefaultCascade()||
				 this.hasNonDefaultAccess()	||
				!this.isDefaultLazy()		||
				!this.isAutoImport()
		;
	}
	
	public boolean hasDefaultPackage() {
		return !StringHelper.isEmpty(defaultPackage);
	}

	public boolean hasSchemaName() {
		return !StringHelper.isEmpty(schemaName);
	}
	
	public boolean hasCatalogName() {
		return !StringHelper.isEmpty(catalogName);
	}
	
	public boolean hasNonDefaultCascade() {
		return !StringHelper.isEmpty(defaultCascade) ? !"none".equals(defaultCascade) ? true : false : false;
	}
	
	public boolean hasNonDefaultAccess() {
		return !StringHelper.isEmpty(defaultAccess) ? !"property".equals(defaultAccess) ? true : false : false;
	}

	public String getSchemaName() {
		return schemaName;
	}

    public String getCatalogName() {
        return catalogName;
    }

	public String getDefaultCascade() {
		return defaultCascade;
	}

	public String getDefaultAccess() {
		return defaultAccess;
	}

	public String getDefaultPackage() {
		return defaultPackage;
	}

	public boolean isDefaultLazy() {
		return defaultLazy;
	}

	/**
	 * Returns the autoImport.
	 * @return boolean
	 */
	public boolean isAutoImport() {
		return autoImport;
	}

	/**
	 * Sets the schemaName.
	 * @param schemaName The schemaName to set
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

    /**
     * Sets the catalogName.
     * @param catalogName The catalogName to set
     */
    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

	/**
	 * Sets the defaultCascade.
	 * @param defaultCascade The defaultCascade to set
	 */
	public void setDefaultCascade(String defaultCascade) {
		this.defaultCascade = defaultCascade;
	}

	/**
	 * sets the default access strategy
	 * @param defaultAccess the default access strategy.
	 */
	public void setDefaultAccess(String defaultAccess) {
		this.defaultAccess = defaultAccess;
	}

	/**
	 * @param defaultPackage The defaultPackage to set.
	 */
	public void setDefaultPackage(String defaultPackage) {
		this.defaultPackage = defaultPackage;
	}

	/**
	 * Sets the autoImport.
	 * @param autoImport The autoImport to set
	 */
	public void setAutoImport(boolean autoImport) {
		this.autoImport = autoImport;
	}
	
	/**
	 * Sets the defaultLazy.
	 * @param defaultLazy The defaultLazy to set
	 */
	public void setDefaultLazy(boolean defaultLazy) {
		this.defaultLazy = defaultLazy;
	}
	
}
