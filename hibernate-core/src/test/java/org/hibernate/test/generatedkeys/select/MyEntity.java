package org.hibernate.test.generatedkeys.select;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class MyEntity {
	private Long id;
	private String name;

	public MyEntity() {
	}

	public MyEntity(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
