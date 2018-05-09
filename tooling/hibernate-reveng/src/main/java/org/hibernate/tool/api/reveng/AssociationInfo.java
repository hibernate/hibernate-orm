package org.hibernate.tool.api.reveng;

public interface AssociationInfo {

		String getCascade();	
		String getFetch();		
		Boolean getUpdate();
		Boolean getInsert();					

}
