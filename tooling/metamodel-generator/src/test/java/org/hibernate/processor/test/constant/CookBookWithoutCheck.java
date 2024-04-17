package org.hibernate.processor.test.constant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "#findGoodBooks",
		query = "from CookBookWithoutCheck where bookType = org.hibernate.processor.test.constant.NumericBookType.GOOD")
public class CookBookWithoutCheck {

	@Id
	String isbn;
	String title;
	int bookType;
}
