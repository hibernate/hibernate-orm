package org.hibernate.userguide.hql;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
//tag::hql-examples-domain-model-example[]
@Entity
public class Partner {

	@Id
	@GeneratedValue
	private Long id;

	@Version
	private int version;

	private String name;

	//Getters and setters are omitted for brevity

//end::hql-examples-domain-model-example[]
	public Partner() {
	}

	public Partner(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
//tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]