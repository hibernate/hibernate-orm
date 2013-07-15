package org.hibernate.test.readonly;


/**
 * todo: describe Info
 *
 * @author Steve Ebersole, Gail Badner (adapted this from "proxy" tests version)
 */
public class Info {
	private Long id;
	private String details;

	public Info() {
	}

	public Info(String details) {
		this.details = details;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}
}
