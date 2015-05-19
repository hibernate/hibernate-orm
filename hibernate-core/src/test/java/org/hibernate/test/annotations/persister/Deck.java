/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
