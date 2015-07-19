package org.hibernate.test.schemafilter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "the_entity_3", schema = "the_schema_2")
public class Schema2Entity3 {

	@Id
	private long id;

	public long getId() {
		return id;
	}

	public void setId( long id ) {
		this.id = id;
	}
}
