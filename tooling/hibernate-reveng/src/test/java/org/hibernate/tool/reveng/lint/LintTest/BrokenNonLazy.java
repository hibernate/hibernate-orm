package org.hibernate.tool.reveng.lint.LintTest;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class BrokenNonLazy {

	@Id
	long id;

	public BrokenNonLazy(long id) {
		this.id = id;
	}
}
