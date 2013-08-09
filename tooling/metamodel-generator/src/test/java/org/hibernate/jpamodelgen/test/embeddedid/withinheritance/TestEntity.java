package org.hibernate.jpamodelgen.test.embeddedid.withinheritance;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * @author Hardy Ferentschik
 */
@Entity
@Immutable
@Table(name = "ENTITY")
public class TestEntity implements Serializable {
	@EmbeddedId
	private Ref ref;

	@Column(name = "NAME", insertable = false, updatable = false, unique = true)
	private String name;
}


