package org.hibernate.test.annotations.persister;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Persister;


/**
 * @author Shawn Clowater
 */
@Entity
@org.hibernate.annotations.Entity( persister = "org.hibernate.persister.entity.SingleTableEntityPersister" )
@Persister( impl = org.hibernate.test.annotations.persister.EntityPersister.class )
public class Deck implements Serializable {
	@Id
	public Integer id;

	@OneToMany( mappedBy = "deck" )
	@Persister( impl = org.hibernate.test.annotations.persister.CollectionPersister.class )
	public Set<Card> cards;
}