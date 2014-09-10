/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
