package org.hibernate.ejb.test.mapping;

import java.io.Serializable;

public class HybridHbm implements Serializable {

	private static final long serialVersionUID = -5890349012654277442L;

	private String id;
	private String text;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
