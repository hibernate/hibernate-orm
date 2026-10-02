package org.hibernate.orm.test.join;

/**
 * @author Gail Badner
 */
public class BlogEntry extends Reportable
{
	private String detail;

	public String getDetail() {
		return detail;
	}
	public void setDetail(String detail) {
		this.detail = detail;
	}
}
