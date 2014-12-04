package org.hibernate.test.bytecode.enhancement.transform;

import javax.persistence.Embeddable;

@Embeddable
public class ChildKey {
	String parent;
	String type;
}
