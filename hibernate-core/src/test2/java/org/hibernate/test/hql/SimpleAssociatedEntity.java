/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;


/**
 * @author Steve Ebersole
 */
public class SimpleAssociatedEntity {
	private Long id;
	private String name;
	private SimpleEntityWithAssociation owner;

	public SimpleAssociatedEntity() {
	}

	public SimpleAssociatedEntity(String name) {
		this.name = name;
	}

	public SimpleAssociatedEntity(String name, SimpleEntityWithAssociation owner) {
		this( name );
		bindToOwner( owner );
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SimpleEntityWithAssociation getOwner() {
		return owner;
	}

	public void setOwner(SimpleEntityWithAssociation owner) {
		this.owner = owner;
	}

	public void bindToOwner(SimpleEntityWithAssociation owner) {
		if ( owner != this.owner ) {
			unbindFromCurrentOwner();
			if ( owner != null ) {
				owner.getAssociatedEntities().add( this );
			}
		}
		this.owner = owner;
	}

	public void unbindFromCurrentOwner() {
		if ( this.owner != null ) {
			this.owner.getAssociatedEntities().remove( this );
			this.owner = null;
		}
	}
}
