/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.annotations.entity;

import java.sql.Types;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Where;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * Use hibernate specific annotations
 *
 * @author Emmanuel Bernard
 */
@Entity
@BatchSize(size = 5)
@SelectBeforeUpdate
@DynamicInsert @DynamicUpdate
@OptimisticLocking(type = OptimisticLockType.ALL)
@Polymorphism(type = PolymorphismType.EXPLICIT)
@Where(clause = "1=1")
@FilterDef(name = "minLength", parameters = {@ParamDef(name = "minLength", type = Integer.class)})
@Filter(name = "betweenLength")
@Filter(name = "minLength", condition = ":minLength <= length")
@org.hibernate.annotations.Table(appliesTo = "Forest",
		indexes = {@Index(name = "idx", columnNames = {"name", "length"})})
public class Forest {
	private Integer id;
	private String name;
	private long length;
	private String longDescription;
	private String smallText;
	private String bigText;
	private Country country;
	private Set near;
	
	@OptimisticLock(excluded=true)
	@JdbcTypeCode( Types.LONGVARCHAR )
	@Column(length = 10000)
	public String getLongDescription() {
		return longDescription;
	}

	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}

	public long getLength() {
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	@Id
	@GeneratedValue
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

	@Convert( converter = ToLowerConverter.class )
	public String getSmallText() {
		return smallText;
	}

	@Convert( converter = ToUpperConverter.class )
	public String getBigText() {
		return bigText;
	}

	public void setSmallText(String smallText) {
		this.smallText = smallText;
	}

	public void setBigText(String bigText) {
		this.bigText = bigText;
	}

	@Lob
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@Lob
	@ElementCollection
	public Set<Country> getNear() {
		return near;
	}

	public void setNear(Set<Country>near) {
		this.near = near;
	}
	
}
