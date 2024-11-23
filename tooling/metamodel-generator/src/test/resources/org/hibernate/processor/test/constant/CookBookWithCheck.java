package org.hibernate.processor.test.constant;

import org.hibernate.annotations.processing.CheckHQL;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@Entity
@CheckHQL
@NamedQuery(name = "#findGoodBooks",
		query = "from CookBookWithCheck where bookType = org.hibernate.processor.test.constant.CookBookWithCheck.GOOD")
@NamedQuery(name = "#findBadBooks",
		query = "from CookBookWithCheck where bookType = 0")
public class CookBookWithCheck {

	public static final int GOOD = 1;
	public static final int BAD = 0;
	public static final int UGLY = -1;

	@Id
	String isbn;
	String title;
	int bookType;
}
