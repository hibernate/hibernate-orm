/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.dialect.Dialect;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A mapping model object representing an {@linkplain jakarta.persistence.Index index} on a relational database table.
 * <p>
 * We regularize the semantics of unique constraints on nullable columns: two null values are not considered to be
 * "equal" for the purpose of determining uniqueness, just as specified by ANSI SQL and common sense.
 *
 * @author Gavin King
 */
public class Index implements Exportable, Serializable {
	private Identifier name;
	private Table table;
	private boolean unique;
	private String options = "";
	private final java.util.List<Selectable> selectables = new ArrayList<>();
	private final java.util.Map<Selectable, String> selectableOrderMap = new HashMap<>();

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isUnique() {
		return unique;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public int getColumnSpan() {
		return selectables.size();
	}

	public List<Selectable> getSelectables() {
		return unmodifiableList( selectables );
	}

	public Map<Selectable, String> getSelectableOrderMap() {
		return unmodifiableMap( selectableOrderMap );
	}

	/**
	 * @deprecated use {@link #getSelectables()}
	 */
	@Deprecated(since = "6.3")
	public java.util.List<Column> getColumns() {
		return selectables.stream().map( selectable -> (Column) selectable ).toList();
	}

	/**
	 * @deprecated use {@link #getSelectableOrderMap()}
	 */
	@Deprecated(since = "6.3")
	public java.util.Map<Column, String> getColumnOrderMap() {
		return selectableOrderMap.entrySet().stream()
				.collect( toUnmodifiableMap( e -> (Column) e.getKey(), Map.Entry::getValue ) );
	}

	public void addColumn(Selectable selectable) {
		if ( !selectables.contains( selectable ) ) {
			selectables.add( selectable );
		}
	}

	public void addColumn(Selectable selectable, String order) {
		addColumn( selectable );
		if ( isNotEmpty( order ) ) {
			selectableOrderMap.put( selectable, order );
		}
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getName() + ")";
	}

	@Override
	public String getExportIdentifier() {
		return qualify( getTable().getExportIdentifier(), "IDX-" + getName() );
	}
}
