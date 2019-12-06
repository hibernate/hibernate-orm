package org.hibernate.tool.internal.reveng;

import org.hibernate.tool.api.reveng.AssociationInfo;

public class DefaultAssociationInfo implements AssociationInfo {
	
	public static DefaultAssociationInfo create(
			String cascade, 
			String fetch, 
			Boolean insert, 
			Boolean update) {
		DefaultAssociationInfo result = new DefaultAssociationInfo();
		result.cascade = cascade;
		result.fetch = fetch;
		result.insert = insert;
		result.update = update;
		return result;
	}
	
	private DefaultAssociationInfo() {}

	private String cascade;
	private String fetch;
	private Boolean insert;
	private Boolean update;
	
	public String getCascade() {
		return cascade;
	}
	public void setCascade(String cascade) {
		this.cascade = cascade;
	}
	public String getFetch() {
		return fetch;
	}
	public void setFetch(String fetch) {
		this.fetch = fetch;
	}
	public Boolean getInsert() {
		return insert;
	}
	public void setInsert(Boolean insert) {
		this.insert = insert;
	}
	public Boolean getUpdate() {
		return update;
	}
	public void setUpdate(Boolean update) {
		this.update = update;
	}

}
