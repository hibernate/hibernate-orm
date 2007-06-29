package org.hibernate.mapping;

/**
 * Common interface for things that can handle meta attributes.
 * 
 * @since 3.0.1
 */
public interface MetaAttributable {

	public java.util.Map getMetaAttributes();

	public void setMetaAttributes(java.util.Map metas);
		
	public MetaAttribute getMetaAttribute(String name);

}
