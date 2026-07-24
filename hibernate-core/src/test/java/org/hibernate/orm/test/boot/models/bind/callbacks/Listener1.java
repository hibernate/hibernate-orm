package org.hibernate.orm.test.boot.models.bind.callbacks;

import jakarta.persistence.EntityListener;
import jakarta.persistence.PostLoad;

/**
 * @author Steve Ebersole
 */
@EntityListener
public class Listener1 {
	@PostLoad
	public void wasLoaded(HierarchySuper entity) {
	}
}
