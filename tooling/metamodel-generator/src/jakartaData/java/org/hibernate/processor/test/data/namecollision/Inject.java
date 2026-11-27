package org.hibernate.processor.test.data.namecollision;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Inject {
	@Id
	Long id;
}
