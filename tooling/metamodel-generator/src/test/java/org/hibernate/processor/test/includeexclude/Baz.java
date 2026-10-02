package org.hibernate.processor.test.includeexclude;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.processing.Exclude;

@Exclude
@Entity
public class Baz {
	@Id long id;
}
