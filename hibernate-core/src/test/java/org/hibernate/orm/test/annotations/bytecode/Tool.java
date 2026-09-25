package org.hibernate.orm.test.annotations.bytecode;

/**
 * @author Emmanuel Bernard
 */
public interface Tool {
	Long getId();
	void setId(Long id);

	Number usage();
}
