package org.hibernate.test.component.cascading.collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Definition {
    private Long id;
	private Set values = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set getValues() {
		return values;
	}

	public void setValues(Set values) {
		this.values = values;
	}
}
