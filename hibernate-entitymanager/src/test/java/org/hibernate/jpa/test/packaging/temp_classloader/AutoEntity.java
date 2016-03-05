package org.hibernate.jpa.test.packaging.temp_classloader;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Janario Oliveira
 */
@Entity
public class AutoEntity {
	@Id
	private Integer id;
}
