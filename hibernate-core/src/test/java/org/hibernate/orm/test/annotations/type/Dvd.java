/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.type;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dvd {
	private MyOid id;
	private String title;

	@EmbeddedId
	@GeneratedValue(generator = "custom-id")
	@GenericGenerator(name = "custom-id", type = MyOidGenerator.class)
	@AttributeOverride(name = "aHigh", column = @Column(name = "high"))
	@AttributeOverride(name = "aMiddle", column = @Column(name = "middle"))
	@AttributeOverride(name = "aLow", column = @Column(name = "low"))
	@AttributeOverride(name = "aOther", column = @Column(name = "other"))
	public MyOid getId() {
		return id;
	}

	public void setId(MyOid id) {
		this.id = id;
	}

	@Column(name="`title`")
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
