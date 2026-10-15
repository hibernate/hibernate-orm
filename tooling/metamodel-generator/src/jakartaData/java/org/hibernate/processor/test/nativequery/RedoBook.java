package org.hibernate.processor.test.nativequery;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class RedoBook {

	@Id
	Long id;

	String title;
}
