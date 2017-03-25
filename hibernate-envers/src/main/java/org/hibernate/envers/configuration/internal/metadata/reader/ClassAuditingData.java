/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata.reader;

import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.envers.AuditTable;

import static org.hibernate.envers.internal.tools.Tools.newHashMap;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 * @author Hern&aacut;n Chanfreau
 * @author Chris Cranford
 */
public class ClassAuditingData implements AuditedPropertiesHolder {
	private final Map<String, PropertyAuditingData> properties;
	private final Map<String, String> secondaryTableDictionary;

	private AuditTable auditTable;

	/**
	 * True if the class is audited globally (this helps to cover the cases when there are no fields in the class,
	 * but it's still audited).
	 */
	private boolean defaultAudited;

	public ClassAuditingData() {
		properties = newHashMap();
		secondaryTableDictionary = newHashMap();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public void addPropertyAuditingData(String propertyName, PropertyAuditingData auditingData) {
		properties.put( propertyName, auditingData );
	}

	@Override
	public PropertyAuditingData getPropertyAuditingData(String propertyName) {
		return properties.get( propertyName );
	}

	public Iterable<String> getPropertyNames() {
		return properties.keySet();
	}

	public Map<String, String> getSecondaryTableDictionary() {
		return secondaryTableDictionary;
	}

	public AuditTable getAuditTable() {
		return auditTable;
	}

	public void setAuditTable(AuditTable auditTable) {
		this.auditTable = auditTable;
	}

	public void setDefaultAudited(boolean defaultAudited) {
		this.defaultAudited = defaultAudited;
	}

	public boolean isAudited() {
		return defaultAudited || properties.size() > 0;
	}

	@Override
	public boolean contains(String propertyName) {
		return properties.containsKey( propertyName );
	}

	public Iterable<String> getNonSyntheticPropertyNames() {
		return properties.entrySet().stream()
				.filter( e -> !e.getValue().isSyntheic() )
				.map( Map.Entry::getKey )
				.collect( Collectors.toList() );
	}

	public Iterable<PropertyAuditingData> getSyntheticProperties() {
		return properties.values().stream()
				.filter( p -> p.isSyntheic() )
				.collect( Collectors.toList() );
	}
}
