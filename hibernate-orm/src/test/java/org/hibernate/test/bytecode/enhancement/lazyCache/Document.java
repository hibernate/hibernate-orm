/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazyCache;

import java.util.Date;
import java.util.Locale;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Formula;

/**
 * @author Gavin King
 */
@Entity
@Cacheable
@Cache( usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy", region = "foo" )
public class Document {
	@Id
	@GeneratedValue
	private Long id;
	private String name;
	@Basic( fetch = FetchType.LAZY )
	@Formula( "upper(name)" )
	private String upperCaseName;
	@Basic( fetch = FetchType.LAZY )
	private String summary;
	@Basic( fetch = FetchType.LAZY )
	private String text;
	@Basic( fetch = FetchType.LAZY )
	private Date lastTextModification;

	Document() {
	}
	
	public Document(String name, String summary, String text) {
		this.lastTextModification = new Date();
		this.name = name;
		this.upperCaseName = name.toUpperCase( Locale.ROOT );
		this.summary = summary;
		this.text = text;
	}

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

	public String getUpperCaseName() {
		return upperCaseName;
	}

	public void setUpperCaseName(String upperCaseName) {
		this.upperCaseName = upperCaseName;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getText() {
		return text;
	}

	private void setText(String text) {
		this.text = text;
	}

	public void updateText(String newText) {
		if ( !newText.equals(text) ) {
			this.text = newText;
			lastTextModification = new Date();
		}
	}

	public Date getLastTextModification() {
		return lastTextModification;
	}
	
}
