package org.hibernate.orm.test.orphan.one2one.fk.reversed.unidirectional;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EmployeeInfo {
	private Long id;

	public EmployeeInfo() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
