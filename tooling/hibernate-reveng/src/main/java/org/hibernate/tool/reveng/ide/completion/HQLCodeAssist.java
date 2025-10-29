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
package org.hibernate.tool.reveng.ide.completion;

import org.hibernate.boot.Metadata;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class HQLCodeAssist implements IHQLCodeAssist {

	private ConfigurationCompletion completion;
	private Metadata metadata;
	
	private static final char[] charSeparators;	

	static {
		charSeparators = new char[]{',', '(', ')'};
		Arrays.sort(charSeparators);
	}
	
	public HQLCodeAssist(Metadata metadata) {
		this.metadata = metadata;
		this.completion = new ConfigurationCompletion(metadata);
	}

	public void codeComplete(String query, int position, IHQLCompletionRequestor collector) {
		
		int prefixStart = findNearestWhiteSpace(query, position);
		String prefix = query.substring( prefixStart, position );
		
		boolean showEntityNames;
		try {
			showEntityNames = new HQLAnalyzer().shouldShowEntityNames( query, position );
		
		if(showEntityNames) {
			if(hasMetadata()) {
				completion.getMatchingImports( prefix, position, collector );				
			} else {
				collector.completionFailure("Configuration not available nor open");
			}
		} else {
			List<EntityNameReference> visible = new HQLAnalyzer().getVisibleEntityNames( query.toCharArray(), position );
			int dotIndex = prefix.lastIndexOf(".");
            if (dotIndex == -1) {
                // It's a simple path, not a dot separated one (find aliases that matches)
            	for (Iterator<EntityNameReference> iter = visible.iterator(); iter.hasNext();) {
					EntityNameReference qt = iter.next();
					String alias = qt.getAlias();
                    if (alias.startsWith(prefix)) {
                    		HQLCompletionProposal completionProposal = new HQLCompletionProposal(HQLCompletionProposal.ALIAS_REF, position);
							completionProposal.setCompletion( alias.substring( prefix.length() ) );
                    		completionProposal.setReplaceStart( position );
                    		completionProposal.setReplaceEnd( position+0 );
                    		completionProposal.setSimpleName( alias );
                    		completionProposal.setShortEntityName( qt.getEntityName() );
                    		if(hasMetadata()) {
                    			String importedName = (String) metadata.getImports().get( qt.getEntityName() );
                    			completionProposal.setEntityName( importedName );
                    		}
                    		collector.accept( completionProposal );
                    }                                        
                }
            } else {
            	if(hasMetadata()) {        			
            		String path = CompletionHelper.getCanonicalPath(visible, prefix.substring(0, dotIndex));
            		String propertyPrefix = prefix.substring(dotIndex + 1);
            		completion.getMatchingProperties( path, propertyPrefix, position, collector );
            	} else {
            		collector.completionFailure("Configuration not available nor open");
            	}
            }
            
            completion.getMatchingFunctions( prefix, position, collector );
            completion.getMatchingKeywords( prefix, position, collector );
            

		}
		} catch(SimpleLexerException sle) {
			collector.completionFailure( "Syntax error: " + sle.getMessage() );
		}
		
	}
	
	private boolean hasMetadata() {
		return metadata!=null;
	}
	
	public static int findNearestWhiteSpace( CharSequence doc, int start ) {
    	boolean loop = true;
    	
    	int offset = 0;
    	
    	int tmpOffset = start - 1;
    	while (loop && tmpOffset >= 0) {
    		char c = doc.charAt(tmpOffset);
    		if(isWhitespace(c)) {
    			loop = false;
    		} else {
    			tmpOffset--;
    		}                        
    	}            
    	offset = tmpOffset + 1;

    	return offset;
    }

	private static boolean isWhitespace(char c) {
		return Arrays.binarySearch(charSeparators, c) >= 0  
				|| Character.isWhitespace(c);
	}

}
