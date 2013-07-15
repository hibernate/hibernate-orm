package org.hibernate.test.deletetransient;


/**
 *
 * @author Gail Badner
 */
public class Note {
	private Long id;
	private String description;

	public Note() {
	}

	public Note(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
