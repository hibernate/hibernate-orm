package org.hibernate.processor.test.data.versioned;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class Versioned {
	@Id long id;
	@Version int version;
}
