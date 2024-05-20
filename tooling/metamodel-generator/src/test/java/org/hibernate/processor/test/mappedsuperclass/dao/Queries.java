package org.hibernate.processor.test.mappedsuperclass.dao;

import org.hibernate.annotations.processing.Find;

public interface Queries {
	@Find
	Child getChild(Long id);
}