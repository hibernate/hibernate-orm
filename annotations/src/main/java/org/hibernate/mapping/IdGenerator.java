//$Id$
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Properties;

/**
 * Identifier generator container,
 * Useful to keep named generator in annotations
 *
 * @author Emmanuel Bernard
 */
public class IdGenerator implements Serializable {
	private String name;
	private String identifierGeneratorStrategy;
	private Properties params = new Properties();


	/**
	 * @return identifier generator strategy
	 */
	public String getIdentifierGeneratorStrategy() {
		return identifierGeneratorStrategy;
	}

	/**
	 * @return generator name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return generator configuration parameters
	 */
	public Properties getParams() {
		return params;
	}

	public void setIdentifierGeneratorStrategy(String string) {
		identifierGeneratorStrategy = string;
	}

	public void setName(String string) {
		name = string;
	}

	public void addParam(String key, String value) {
		params.setProperty( key, value );
	}

}
