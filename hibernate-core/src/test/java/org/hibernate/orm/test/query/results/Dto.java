package org.hibernate.orm.test.query.results;

import org.hibernate.annotations.Imported;

/**
 * @author Steve Ebersole
 */
@Imported
public class Dto {
	private final Integer key;
	private final String text;

	public Dto(Integer key, String text) {
		this.key = key;
		this.text = text;
	}

	public Integer getKey() {
		return key;
	}

	public String getText() {
		return text;
	}
}
