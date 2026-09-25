package org.hibernate.tool.reveng.api.core;

public interface AssociationInfo {

		String getCascade();
		String getFetch();
		Boolean getUpdate();
		Boolean getInsert();

}
