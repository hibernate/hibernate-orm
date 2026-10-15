package org.hibernate.orm.test.instrumentation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/// Kept outside the test class to avoid early loading during JUnit discovery.
///
/// @author Steve Ebersole
@Entity
public class GenericEntity extends GenericMappedSuperclass<String> {

	@Id
	long id;
}
