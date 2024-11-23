/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.BatchSize;

import java.util.Set;

@Entity
@BatchSize( size = 20 )
public class TheEntity {
	@Id
	private Integer id;
	private String name;

	@Embedded
	private TheEmbeddable theEmbeddable;

	@ManyToOne
	@JoinColumn
	private TheEntity theManyToOne;

	@OneToMany( mappedBy = "theManyToOne" )
	private Set<TheEntity> theOneToMany;

	@ElementCollection
	@JoinColumn( name = "owner_id" )
	private Set<TheEmbeddable> theEmbeddableCollection;


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TheEmbeddable getTheEmbeddable() {
		return theEmbeddable;
	}

	public void setTheEmbeddable(TheEmbeddable theEmbeddable) {
		this.theEmbeddable = theEmbeddable;
	}

	public TheEntity getTheManyToOne() {
		return theManyToOne;
	}

	public void setTheManyToOne(TheEntity theManyToOne) {
		this.theManyToOne = theManyToOne;
	}

	public Set<TheEntity> getTheOneToMany() {
		return theOneToMany;
	}

	public void setTheOneToMany(Set<TheEntity> theOneToMany) {
		this.theOneToMany = theOneToMany;
	}

	public Set<TheEmbeddable> getTheEmbeddableCollection() {
		return theEmbeddableCollection;
	}

	public void setTheEmbeddableCollection(Set<TheEmbeddable> theEmbeddableCollection) {
		this.theEmbeddableCollection = theEmbeddableCollection;
	}
}
