/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * @author Chris Cranford
 */
@Embeddable
@Audited
public class Item {
	private String name;

	@ManyToOne
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	private Type type;

	Item() {

	}

	Item(String name, Type type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( type != null ? type.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( object == null || getClass() != object.getClass() ) {
			return false;
		}

		Item that = (Item) object;
		if ( name != null ? !name.equals( that.name ) : that.name != null ) {
			return false;
		}
		return !( type != null ? !type.equals( that.type ) : that.type != null );
	}
}
