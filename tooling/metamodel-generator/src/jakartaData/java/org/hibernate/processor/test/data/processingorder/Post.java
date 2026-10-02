package org.hibernate.processor.test.data.processingorder;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Post {
	@Id
	Integer id;

	String posterId;

	@ManyToOne
	protected Topic topic;
}
