/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.identifiercollection;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.TableGenerator;

import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.Type;

/**
 * @author Emmanuel Bernard
 */
@Entity
@TableGenerator(name="ids_generator", table="IDS")
public class Passport {
	@Id @GeneratedValue @Column(name="passport_id") private Long id;
	private String name;

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name="PASSPORT_STAMP")
	@CollectionId(columns = @Column(name="COLLECTION_ID"), type=@Type(type="long"), generator = "generator")
	@TableGenerator(name="generator", table="IDSTAMP")
	private Collection<Stamp> stamps = new ArrayList();

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name="PASSPORT_VISASTAMP")
	@CollectionId(columns = @Column(name="COLLECTION_ID"), type=@Type(type="long"), generator = "ids_generator")
	//TODO test identity generator
	private Collection<Stamp> visaStamp = new ArrayList();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Collection<Stamp> getStamps() {
		return stamps;
	}

	public void setStamps(Collection<Stamp> stamps) {
		this.stamps = stamps;
	}

	public Collection<Stamp> getVisaStamp() {
		return visaStamp;
	}

	public void setVisaStamp(Collection<Stamp> visaStamp) {
		this.visaStamp = visaStamp;
	}
}
