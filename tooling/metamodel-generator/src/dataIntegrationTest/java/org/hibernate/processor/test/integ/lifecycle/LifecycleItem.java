package org.hibernate.processor.test.integ.lifecycle;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class LifecycleItem {
	@Id
	Long id;
	String name;
}
