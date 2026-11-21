package org.hibernate.tool.reveng.lint.LintTest;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class FakeNonLazy {

	@Id
	@GeneratedValue
	private long id;
}
