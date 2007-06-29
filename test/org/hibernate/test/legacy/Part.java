//$Id$
package org.hibernate.test.legacy;

/**
 * @author Gavin King
 */
public class Part {

	private Long id;
	private String description;
	
	public String getDescription() {
		return description;
	}

	public Long getId() {
		return id;
	}

	public void setDescription(String string) {
		description = string;
	}

	public void setId(Long long1) {
		id = long1;
	}
	
	public static class SpecialPart extends Part {}

}
