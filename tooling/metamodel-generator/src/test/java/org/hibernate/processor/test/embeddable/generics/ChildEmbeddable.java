package org.hibernate.processor.test.embeddable.generics;

import jakarta.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class ChildEmbeddable extends ParentEmbeddable<MyTypeImpl> {
}
