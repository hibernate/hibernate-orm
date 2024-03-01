package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Thomas Heigl
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Like<T extends Like.I1 & Like.I2> {

	@Id
	private Long id;

	public abstract Reference<T> getObject();

	interface I1 {
	}

	interface I2 {
	}

	public static class Reference<T> {
	}

}
