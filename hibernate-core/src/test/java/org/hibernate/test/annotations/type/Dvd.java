//$Id$
package org.hibernate.test.annotations.type;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dvd {
	private MyOid id;
	private String title;

	@Id
	@GeneratedValue(generator = "custom-id")
	@GenericGenerator(name = "custom-id", strategy = "org.hibernate.test.annotations.type.MyOidGenerator")
	@Type(type = "org.hibernate.test.annotations.type.MyOidType")
	@Columns(
			columns = {
			@Column(name = "high"),
			@Column(name = "middle"),
			@Column(name = "low"),
			@Column(name = "other")
					}
	)
	public MyOid getId() {
		return id;
	}

	public void setId(MyOid id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
