package org.hibernate.jpa.test.packaging.temp_classloader;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Janario Oliveira
 */
@Entity
public class MyEntity {
	@Id
	private Integer id;
}
