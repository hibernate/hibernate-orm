package org.hibernate.test.onetomany.inheritance;

import java.util.Map;

public interface Library {
	int getEntid();
	
	Map<String,Book> getBooksOnInventory();
}
