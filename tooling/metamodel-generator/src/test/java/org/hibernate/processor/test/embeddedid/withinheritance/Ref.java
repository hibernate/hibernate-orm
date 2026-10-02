package org.hibernate.processor.test.embeddedid.withinheritance;

/**
 * @author Hardy Ferentschik
 */
public class Ref extends AbstractRef {
	public Ref() {
	}

	public Ref(int id) {
		super( id );
	}
}
