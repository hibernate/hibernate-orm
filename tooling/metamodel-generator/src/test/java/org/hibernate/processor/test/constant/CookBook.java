package org.hibernate.processor.test.constant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity
@NamedQuery(name = "#findGoodBooks",
		query = "from CookBook where bookType = org.hibernate.processor.test.constant.BookType.GOOD_BOOK")
public class CookBook {

	@Id
	String isbn;
	String title;
	BookType bookType;
}
