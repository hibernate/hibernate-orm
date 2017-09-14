package org.hibernate.test.annotations.onetomany;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class F {

	@Id
	Long id;

	@OneToMany(cascade = CascadeType.ALL)
	@OnDelete(action = OnDeleteAction.CASCADE)
	List<E> listOfEs;
}
