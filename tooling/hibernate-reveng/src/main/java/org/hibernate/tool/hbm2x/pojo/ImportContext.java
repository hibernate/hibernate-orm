package org.hibernate.tool.hbm2x.pojo;

public interface ImportContext {

	/**
	 * Add fqcn to the import list. Returns fqcn as needed in source code.
	 * Attempts to handle fqcn with array and generics references.
	 * 
	 * e.g.
	 * java.util.Collection<org.marvel.Hulk> imports java.util.Collection and returns Collection
	 * org.marvel.Hulk[] imports org.marvel.Hulk and returns Hulk
	 * 
	 * 
	 * @param fqcn
	 * @return import string
	 */
	public abstract String importType(String fqcn);

	public abstract String staticImport(String fqcn, String member);
	
	public abstract String generateImports();

}