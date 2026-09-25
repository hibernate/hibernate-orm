package org.hibernate.orm.test.inheritance.embeddable;


import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
@Embeddable
public class SimpleEmbeddable {
	private String data;

	public SimpleEmbeddable() {
	}

	public SimpleEmbeddable(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}
}
