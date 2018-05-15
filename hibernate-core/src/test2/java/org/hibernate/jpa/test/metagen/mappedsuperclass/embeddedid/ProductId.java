/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.embeddedid;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Justin Wesley
 * @author Steve Ebersole
 */
@Embeddable
public class ProductId implements Serializable {
	private Integer id;
	private String code;

	public ProductId() {
	}

	@Column
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}
