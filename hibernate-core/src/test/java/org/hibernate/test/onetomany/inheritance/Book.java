package org.hibernate.test.onetomany.inheritance;

public interface Book extends Product {
	String getIsbn();
	
	Library getLibrary();
	
	void setLibrary(Library arg);
}
