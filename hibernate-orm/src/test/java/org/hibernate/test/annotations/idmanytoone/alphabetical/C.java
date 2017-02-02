/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.idmanytoone.alphabetical;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;

@Entity
@IdClass( CId.class )
public class C {

	@Id
	private A parent;

	@Id
	private int sequenceNumber;

	@OneToMany( mappedBy = "parent" )
	List<B> children;

	public C() {
	}

	public A getParent() {
		return parent;
	}

	public void setParent(A parent) {
		this.parent = parent;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public List<B> getChildren() {
		return children;
	}

	public void setChildren(List<B> children) {
		this.children = children;
	}


}

