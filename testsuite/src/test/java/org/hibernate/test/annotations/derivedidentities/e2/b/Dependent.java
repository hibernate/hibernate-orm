package org.hibernate.test.annotations.derivedidentities.e2.b;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent {
	@EmbeddedId
	DependentId id;

	@MapsId("empPK")
//	@JoinColumns({
//			@JoinColumn(name = "FK1", referencedColumnName = "firstName"),
//			@JoinColumn(name = "FK2", referencedColumnName = "lastName")
//	})
	@ManyToOne
	Employee emp;
}
