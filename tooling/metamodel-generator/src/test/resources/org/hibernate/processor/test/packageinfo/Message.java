package org.hibernate.processor.test.packageinfo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Message {

	@Id
	Integer id;

	String key;
}
