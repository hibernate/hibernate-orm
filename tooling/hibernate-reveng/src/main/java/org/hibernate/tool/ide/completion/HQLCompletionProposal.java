package org.hibernate.tool.ide.completion;

import org.hibernate.mapping.Property;



public class HQLCompletionProposal {
	
	static final char[] NO_CHAR = new char[0];
	
	public static final int ENTITY_NAME = 1;
	public static final int PROPERTY = 2;
	public static final int KEYWORD = 3;
	public static final int FUNCTION = 4;
	public static final int ALIAS_REF = 5; // ref to an alias name, e.g. "bar" in "from Bar as bar where b|"
	
	protected static final int FIRST_KIND = ENTITY_NAME;
	protected static final int LAST_KIND = ALIAS_REF;
	
	/**
	 * kind of completion request.
	 */
	private int completionKind;
	
	/**
	 * original cursorposition in the query
	 */
	private int completionLocation;
	
	/**
	 * The actual completion. 
	 */
	private String completion = "";
	
	private int replaceStart = 0;	
	private int replaceEnd = 0;
	
	/**
	 * Relevance rating
	 */
	private int relevance = 1;

	/** The default name for the entityname, keyword, property etc. */
	private String simpleName = "";
	
	/** The full related entity name, the resolved shortEntityName. Can be null */
	private String entityName = null;
	
	/** 
	 * A short entity name. e.g. the imported name. 
	 * e.g. "Product" instead of "org.hibernate.model.Product" 
	 * (note: a imported name can also be the long version) 
	 **/
	private String shortEntityName = null;
	
	/**
	 * The propertyName, can be null.  
	 */
	private String propertyName = null;

	/**
	 * The underlying property. Can be null.
	 */
	private Property property;
	
	public String getCompletion() {
		return completion;
	}

	public void setCompletion(String completion) {
		this.completion = completion;
	}

	public int getCompletionKind() {
		return completionKind;
	}

	public void setCompletionKind(int completionKind) {
		this.completionKind = completionKind;
	}

	public int getCompletionLocation() {
		return completionLocation;
	}

	public void setCompletionLocation(int completionLocation) {
		this.completionLocation = completionLocation;
	}

	public int getRelevance() {
		return relevance;
	}

	public void setRelevance(int relevance) {
		this.relevance = relevance;
	}

	public int getReplaceEnd() {
		return replaceEnd;
	}

	public void setReplaceEnd(int replaceEnd) {
		this.replaceEnd = replaceEnd;
	}

	public int getReplaceStart() {
		return replaceStart;
	}

	public void setReplaceStart(int replaceStart) {
		this.replaceStart = replaceStart;
	}
	
	public HQLCompletionProposal(int kind, int cursorPosition) {
		this.completionKind = kind;
		this.completionLocation = cursorPosition; 
	}
		
	public String getSimpleName() {
		return simpleName;
	}

	public void setSimpleName(String simpleName) {
		this.simpleName = simpleName;
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append('[');
		switch(this.completionKind) {
			case ENTITY_NAME :
				buffer.append("ENTITY_NAME"); 
				break;
			case PROPERTY:
				buffer.append("PROPERTY");
				break;
			case KEYWORD:
				buffer.append("KEYWORD");
				break;
			default :
				buffer.append("<Unknown type>");
				break;
				
		}
		buffer.append("]{completion:"); //$NON-NLS-1$
		if (this.completion != null) buffer.append(this.completion);
		buffer.append(", simpleName:"); //$NON-NLS-1$
		if (this.simpleName != null) buffer.append(this.simpleName);
		buffer.append(", ["); //$NON-NLS-1$
		buffer.append(this.replaceStart);
		buffer.append(',');
		buffer.append(this.replaceEnd);
		buffer.append("], relevance="); //$NON-NLS-1$
		buffer.append(this.relevance);
		buffer.append('}');
		return buffer.toString();
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getShortEntityName() {
		return shortEntityName;
	}

	public void setShortEntityName(String shortEntityName) {
		this.shortEntityName = shortEntityName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setProperty(Property element) {
		this.property = element;		
	}

	public Property getProperty() {
		return property;
	}
	

	
}
