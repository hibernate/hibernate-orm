package org.hibernate.ejb.test.metadata;

import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Cat extends Cattish {
	private String nickname;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
