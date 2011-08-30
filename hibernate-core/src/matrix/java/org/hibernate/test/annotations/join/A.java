//$Id$
package org.hibernate.test.annotations.join;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

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
