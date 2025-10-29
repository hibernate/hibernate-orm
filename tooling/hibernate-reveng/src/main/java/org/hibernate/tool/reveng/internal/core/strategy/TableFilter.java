/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.apache.commons.collections4.MultiValuedMap;
import org.hibernate.tool.reveng.api.core.TableIdentifier;
import org.hibernate.tool.reveng.internal.core.strategy.MetaAttributeHelper.SimpleMetaAttribute;


/**
 * 
 * A tablefilter that can tell if a TableIdentifier is included or excluded.
 * Note that all matching is case sensitive since many db's are. 
 *  
 * @author max
 *
 */
public class TableFilter {

	// TODO: very basic substring matching. Possibly include regex functionallity ? (jdk 1.4 dep)
	public static class Matcher {
		
		private static final int EQUALS = 1;
		private static final int ENDSWITH = 2;
		private static final int STARTSWITH = 3;
		private static final int SUBSTRING = 4;
		private static final int ANY = 5;
		
		final int mode;
		final String value;
		final String matchValue;
		
		Matcher(String match) {
			matchValue = match;
			if(".*".equals(match) ) {
				mode = ANY;
				value = null;
			} 
			else if(match.length()>4 && match.startsWith(".*") && match.endsWith(".*") ) {
				mode = SUBSTRING;
				value = match.substring(2, match.length()-2);
			}  
			else if(match.endsWith(".*") ) {
				mode = STARTSWITH;
				value = match.substring(0, match.length()-2);
			} 
			else if (match.startsWith(".*") ){
				mode = ENDSWITH;
				value = match.substring(2);
			} 
			else {
				mode = EQUALS;
				value = match;
			}
		}
		
		boolean match(String matchEnum) {
			switch (mode) {
			case ANY: return true;
			case EQUALS: return this.value.equals(matchEnum);
			case ENDSWITH: return matchEnum.endsWith(this.value);
			case STARTSWITH: return matchEnum.startsWith(this.value);
			case SUBSTRING: return matchEnum.indexOf(this.value)>=0;
			default:
				throw new IllegalStateException();				
			}
		}
		
		public String toString() {
			return matchValue;
		}
	}

	private Boolean exclude;
	private String packageName;
	
	private Matcher catalogMatcher;
	private Matcher schemaMatcher;
	private Matcher nameMatcher;
	private MultiValuedMap<String, SimpleMetaAttribute> metaAttributes;

	
	
	public TableFilter() {
		setMatchCatalog(".*");
		setMatchSchema(".*");
		setMatchName(".*");
		setExclude(null);
	}
	
	public void setMatchCatalog(String matchCatalog) {
		this.catalogMatcher = new Matcher(matchCatalog);
	}

	public void setMatchSchema(String matchSchema) {
		this.schemaMatcher = new Matcher(matchSchema);		
	}

	public void setMatchName(String matchName) {
		this.nameMatcher = new Matcher(matchName);
	}

	/**
	 * 
	 * @return null if filter does not affect this identifier, true/false if it does.
	 */
	public Boolean exclude(TableIdentifier identifier) {
		return isRelevantFor(identifier) ? exclude : null;
	}
	
	public void setExclude(Boolean bool) {
		exclude = bool;		
	}

	public String getPackage(TableIdentifier identifier) {
		return isRelevantFor(identifier) ? packageName : null;
	}
	
	private boolean isRelevantFor(TableIdentifier identifier) {
		if(catalogMatcher.match(identifier.getCatalog() ) ) {
			if(schemaMatcher.match(identifier.getSchema() ) ) {
				if(nameMatcher.match(identifier.getName() ) ) {
					return true;
				}	
			}
		}
		return false;
	}

	public void setPackage(String string) {
		packageName = string;
	}
	
	public String toString() {
		return catalogMatcher + " " + schemaMatcher + " " + nameMatcher + " " + exclude;  
	}
	
	public String getMatchCatalog() {
		return catalogMatcher.matchValue;
	}
	
	public String getMatchSchema() {
		return schemaMatcher.matchValue;
	}
	
	public String getMatchName() {
		return nameMatcher.matchValue;
	}
	
	public Boolean getExclude() {
		return exclude;
	}

	public MultiValuedMap<String, SimpleMetaAttribute> getMetaAttributes(TableIdentifier identifier) {
		return isRelevantFor(identifier) ? metaAttributes : null;	
	}
	
	public void setMetaAttributes(MultiValuedMap<String, SimpleMetaAttribute> metaAttributes) {
		this.metaAttributes = metaAttributes;
	}
}
