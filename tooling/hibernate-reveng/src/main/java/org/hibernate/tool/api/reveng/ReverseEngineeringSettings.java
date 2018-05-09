package org.hibernate.tool.api.reveng;

public class ReverseEngineeringSettings {

	
	final ReverseEngineeringStrategy rootStrategy;
	
	private String defaultPackageName = "";
	private boolean detectOptimisticLock = true;
	private boolean createCollectionForForeignKey = true;
	private boolean createManyToOneForForeignKey = true;
	private boolean detectManyToMany = true;
	private boolean detectOneToOne = true;

	
	public ReverseEngineeringSettings(ReverseEngineeringStrategy rootStrategy) {
		this.rootStrategy = rootStrategy;
	}
	
	public ReverseEngineeringSettings setDefaultPackageName(String defaultPackageName) {
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
	
	public ReverseEngineeringSettings setDetectOptimisticLock(
			boolean optimisticLockSupportEnabled) {
		this.detectOptimisticLock = optimisticLockSupportEnabled;
		return this;
	}

	/** if true, a collection will be mapped for each foreignkey */
	public boolean createCollectionForForeignKey() {
		return createCollectionForForeignKey;
	}
	
	
	public ReverseEngineeringSettings setCreateCollectionForForeignKey(
			boolean createCollectionForForeignKey) {
		this.createCollectionForForeignKey = createCollectionForForeignKey;
		return this;
	}

	/** if true, a many-to-one association will be created for each foreignkey found */
	public boolean createManyToOneForForeignKey() {
		return createManyToOneForForeignKey;
	}
	
	public ReverseEngineeringSettings setCreateManyToOneForForeignKey(
			boolean createManyToOneForForeignKey) {
		this.createManyToOneForForeignKey = createManyToOneForForeignKey;
		return this;
	}

	public ReverseEngineeringSettings setDetectManyToMany(boolean b) {
		this.detectManyToMany = b;
		return this;
	}
	
	public boolean getDetectManyToMany() {
		return detectManyToMany;
	}
	
	public ReverseEngineeringSettings setDetectOneToOne(boolean b) {
		this.detectOneToOne = b;
		return this;
	}

	public boolean getDetectOneToOne() {
		return detectOneToOne;
	}
	
	/** return the top/root strategy. Allows a lower strategy to ask another question. Be aware of possible recursive loops; e.g. do not call the root.tableToClassName in tableToClassName of a custom reversengineeringstrategy. */
	public ReverseEngineeringStrategy getRootStrategy() {
		return rootStrategy;
	}

	
	
}
