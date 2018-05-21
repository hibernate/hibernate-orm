package org.hibernate.tool.internal.reveng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.TableIdentifier;

public abstract class AbstractDatabaseCollector implements DatabaseCollector {

	private Map<String, List<ForeignKey>> oneToManyCandidates;
	protected final Map<TableIdentifier, String> suggestedIdentifierStrategies;
	private MetaDataDialect metaDataDialect;

	public AbstractDatabaseCollector(MetaDataDialect metaDataDialect) {
		suggestedIdentifierStrategies = new HashMap<TableIdentifier, String>();
		this.metaDataDialect = metaDataDialect;
	}
	
	public void setOneToManyCandidates(Map<String, List<ForeignKey>> oneToManyCandidates) {
		this.oneToManyCandidates = oneToManyCandidates;
	}

	public Map<String, List<ForeignKey>> getOneToManyCandidates() {
		return oneToManyCandidates;
	}

	public String getSuggestedIdentifierStrategy(String catalog, String schema, String name) {
		TableIdentifier identifier = new TableIdentifier(catalog, schema, name);
		return (String) suggestedIdentifierStrategies.get(identifier);
	}

	public void addSuggestedIdentifierStrategy(String catalog, String schema, String name, String idstrategy) {
		TableIdentifier identifier = new TableIdentifier(catalog, schema, name);
		suggestedIdentifierStrategies.put(identifier, idstrategy);
	}
	
	protected String quote(String name) {
		if (name == null)
			return name;
		if (metaDataDialect.needQuote(name)) {
			if (name.length() > 1 && name.charAt(0) == '`'
					&& name.charAt(name.length() - 1) == '`') {
				return name; // avoid double quoting
			}
			return "`" + name + "`";
		} else {
			return name;
		}
	}

}
