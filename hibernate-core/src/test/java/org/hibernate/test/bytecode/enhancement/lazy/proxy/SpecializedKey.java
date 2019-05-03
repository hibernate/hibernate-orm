/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity(name="SpecializedKey")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(name="PP_PartnerDCKey")
public class SpecializedKey extends GenericKey implements Serializable
{

	@OneToMany(targetEntity= SpecializedEntity.class, mappedBy="specializedKey", fetch=FetchType.LAZY)
//	@LazyCollection( LazyCollectionOption.EXTRA )
	protected Set<SpecializedEntity> specializedEntities = new LinkedHashSet();

	public Set<SpecializedEntity> getSpecializedEntities() {
		return specializedEntities;
	}

	public void setSpecializedEntities(Set<SpecializedEntity> specializedEntities) {
		this. specializedEntities = specializedEntities;
	}

	public void addSpecializedEntity(SpecializedEntity pPartnerZusatzkuerzelZR) {
		this.specializedEntities.add( pPartnerZusatzkuerzelZR);
	}

}
