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
package org.hibernate.tool.api.reveng;

public class RevengSettings {

	
	final RevengStrategy rootStrategy;
	
	private String defaultPackageName = "";
	private boolean detectOptimisticLock = true;
	private boolean createCollectionForForeignKey = true;
	private boolean createManyToOneForForeignKey = true;
	private boolean detectManyToMany = true;
	private boolean detectOneToOne = true;

	
	public RevengSettings(RevengStrategy rootStrategy) {
		this.rootStrategy = rootStrategy;
	}
	
	public RevengSettings setDefaultPackageName(String defaultPackageName) {
		if(defaultPackageName==null) {
			this.defaultPackageName = "";
		} else {
			this.defaultPackageName= defaultPackageName.trim();
		}
		return this;
	}
	
	/** return the default packageName. Never null, at least the empty string */
	public String getDefaultPackageName() {
		return defaultPackageName;
	}
	
	/** If true, reverse engineering strategy will try and autodetect columns for optimistc locking, e.g. VERSION and TIMESTAMP */
	public boolean getDetectOptimsticLock() {
		return detectOptimisticLock ;
	}
	
	public RevengSettings setDetectOptimisticLock(
			boolean optimisticLockSupportEnabled) {
		this.detectOptimisticLock = optimisticLockSupportEnabled;
		return this;
	}

	/** if true, a collection will be mapped for each foreignkey */
	public boolean createCollectionForForeignKey() {
		return createCollectionForForeignKey;
	}
	
	
	public RevengSettings setCreateCollectionForForeignKey(
			boolean createCollectionForForeignKey) {
		this.createCollectionForForeignKey = createCollectionForForeignKey;
		return this;
	}

	/** if true, a many-to-one association will be created for each foreignkey found */
	public boolean createManyToOneForForeignKey() {
		return createManyToOneForForeignKey;
	}
	
	public RevengSettings setCreateManyToOneForForeignKey(
			boolean createManyToOneForForeignKey) {
		this.createManyToOneForForeignKey = createManyToOneForForeignKey;
		return this;
	}

	public RevengSettings setDetectManyToMany(boolean b) {
		this.detectManyToMany = b;
		return this;
	}
	
	public boolean getDetectManyToMany() {
		return detectManyToMany;
	}
	
	public RevengSettings setDetectOneToOne(boolean b) {
		this.detectOneToOne = b;
		return this;
	}

	public boolean getDetectOneToOne() {
		return detectOneToOne;
	}
	
	/** return the top/root strategy. Allows a lower strategy to ask another question. Be aware of possible recursive loops; e.g. do not call the root.tableToClassName in tableToClassName of a custom reversengineeringstrategy. */
	public RevengStrategy getRootStrategy() {
		return rootStrategy;
	}

	
	
}
