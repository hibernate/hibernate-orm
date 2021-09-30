/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.persister;
import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Persister;


/**
 * @author Shawn Clowater
 */
@Entity
@Persister( impl = EntityPersister.class )
public class Deck implements Serializable {
	@Id
	public Integer id;

	@OneToMany( mappedBy = "deck" )
	@Persister( impl = CollectionPersister.class )
	public Set<Card> cards;
}
