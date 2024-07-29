package org.hibernate.tool.internal.reveng.util;

import java.util.Properties;

import org.hibernate.mapping.KeyValue;

public interface EnhancedValue extends KeyValue {

	void setIdentifierGeneratorProperties(Properties suggestedProperties);

	Properties getIdentifierGeneratorProperties();
	
	void setIdentifierGeneratorStrategy(String s);
	
	String getIdentifierGeneratorStrategy();

}
