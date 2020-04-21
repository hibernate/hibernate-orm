/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.inheritance.mixed;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;

@Entity
@DiscriminatorValue("L")
@SecondaryTable(name = "SymbolicLinkMixed")
public class SymbolicLink extends File {

	File target;

	SymbolicLink() {
	}

	public SymbolicLink(File target) {
		this.target = target;
	}

	@ManyToOne(optional = false)
	@JoinColumn(table = "SymbolicLinkMixed")
	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}


}
