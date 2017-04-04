/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.parameterized;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;


/**
 * Our test entity
 *
 * @author Steve Ebersole
 */
@TypeDef(
		name = "DefaultableList", 
		typeClass = DefaultableListType.class,
		parameters = @Parameter(name = "default", value = "Hello" )
)
@javax.persistence.Entity
public class Entity {
	private String name;
	private List values = new ArrayList();

	public Entity() {
	}

	public Entity(String name) {
		this.name = name;
	}

	@Id
	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	@ElementCollection( targetClass = String.class, fetch = FetchType.EAGER )
	@CollectionType( type = "DefaultableList" )
	@JoinColumn( name = "ENT_ID" )
	@OrderColumn( name = "POS" )
	@Column(name = "VAL")
	public List getValues() {
		return values;
	}

	public void setValues(List values) {
		this.values = values;
	}
}
