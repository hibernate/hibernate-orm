//$Id$
package org.hibernate.test.annotations.join;

import java.util.Date;
import javax.persistence.MappedSuperclass;
import javax.persistence.Column;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public abstract class A {
	@Column(nullable = false)
	private Date createDate;


	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
}
