package org.hibernate.cfg.reveng;

public interface ForeignKeyCollectionInfo {

	String getCascade();
	
	String getFetch();
	
	Boolean getUpdate();
	Boolean getInsert();
	
	String getLazy();
	
}


