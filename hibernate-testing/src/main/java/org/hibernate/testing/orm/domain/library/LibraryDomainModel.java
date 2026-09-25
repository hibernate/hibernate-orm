package org.hibernate.testing.orm.domain.library;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;

/**
 * @author Steve Ebersole
 */
public class LibraryDomainModel extends AbstractDomainModelDescriptor {
	public static final LibraryDomainModel INSTANCE = new LibraryDomainModel();

	public LibraryDomainModel() {
		super( Book.class, Person.class );
	}
}
