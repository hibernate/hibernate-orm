package org.hibernate.tool.reveng.guide;

import org.hibernate.tool.reveng.api.core.RevengStrategy;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.strategy.DelegatingStrategy;

/// Customizes generated names and excludes temporary tables while preserving other decisions.
///
/// @author Steve Ebersole
public class GuideStrategy extends DelegatingStrategy {
	public GuideStrategy(RevengStrategy delegate) {
		super( delegate );
	}

	@Override
	public String tableToClassName(TableIdentifier table) {
		return super.tableToClassName( table ) + "Entity";
	}

	@Override
	public boolean excludeTable(TableIdentifier table) {
		return table.getName().startsWith( "TMP_" ) || super.excludeTable( table );
	}
}
