/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */


package org.hibernate.test.annotations.manytoonewithformula;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Sharath Reddy
 */
@Entity
public class Message implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int id;
	private String languageCode;
	private String languageName;
	private Language language;
	
	@Id @GeneratedValue
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	@Column(name="lang_code")
	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String val) {
		this.languageCode = val;
	}

	@Column(name="lang_name")
	public String getLanguageName() {
		return languageName;
	}

	public void setLanguageName(String val) {
		this.languageName = val;
	}
		
	@ManyToOne
	@JoinColumnsOrFormulas(
	{ 
		@JoinColumnOrFormula(formula=@JoinFormula(value="UPPER(lang_code)"))
		//@JoinColumnOrFormula(formula=@JoinFormula(value="(select l.code from Language l where l.name = lang_name)"))
	})
	public Language getLanguage() {
		return language;
	}
	public void setLanguage(Language language) {
		this.language = language;
	}
	
}
