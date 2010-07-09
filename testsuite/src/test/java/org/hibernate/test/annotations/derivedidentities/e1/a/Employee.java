package org.hibernate.test.annotations.derivedidentities.e1.a;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Employee {
	long empId;
	String empName;

	String nickname;

	public Employee() {
	}

	public Employee(long empId, String empName, String nickname) {
		this.empId = empId;
		this.empName = empName;
		this.nickname = nickname;
	}

	@Id
	public long getEmpId() {
		return empId;
	}

	public void setEmpId(long empId) {
		this.empId = empId;
	}

	public String getEmpName() {
		return empName;
	}

	public void setEmpName(String empName) {
		this.empName = empName;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}