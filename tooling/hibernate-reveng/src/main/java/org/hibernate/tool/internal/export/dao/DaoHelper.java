package org.hibernate.tool.internal.export.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.NamedHqlQueryDefinition;

public class DaoHelper {
	
	public static Iterable<NamedHqlQueryDefinition> getNamedHqlQueryDefinitions(Metadata metadata) {
		List<NamedHqlQueryDefinition> result = new ArrayList<NamedHqlQueryDefinition>();
		metadata.visitNamedHqlQueryDefinitions(new Consumer<NamedHqlQueryDefinition>() {			
			@Override
			public void accept(NamedHqlQueryDefinition t) {
				result.add(t);
			}
		});
		return result;
	}

}
