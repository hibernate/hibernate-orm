/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

@Entity
public class Workflow implements Serializable {

	private static final long serialVersionUID = 7504955999101475681L;

	private Long id;
	private Locale defaultLanguage;
	private Set<Locale> supportedLocales = new HashSet<Locale>();
	private Map<Locale, LocalizedEmbeddable> localized = new HashMap<Locale, LocalizedEmbeddable>();

	public Workflow() {
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Basic(optional = false)
	public Locale getDefaultLanguage() {
		return defaultLanguage;
	}

	public void setDefaultLanguage(Locale defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}

	@ElementCollection
	@CollectionTable(joinColumns = { @JoinColumn(name = "ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false) })
	public Set<Locale> getSupportedLocales() {
		return supportedLocales;
	}

	public void setSupportedLocales(Set<Locale> supportedLocales) {
		this.supportedLocales = supportedLocales;
	}

	@ElementCollection
	@CollectionTable(joinColumns = { @JoinColumn(name = "ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false) })
	@MapKeyColumn(name = "LANGUAGE_CODE", nullable = false, insertable = false, updatable = false)
	public Map<Locale, LocalizedEmbeddable> getLocalized() {
		return localized;
	}

	public void setLocalized(Map<Locale, LocalizedEmbeddable> localized) {
		this.localized = localized;
	}

}
