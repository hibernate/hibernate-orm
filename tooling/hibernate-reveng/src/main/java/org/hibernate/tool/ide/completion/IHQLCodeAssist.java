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
package org.hibernate.tool.ide.completion;

/**
 * Interface for code assist on HQL strings.
 * 
 * @author Max Rydahl Andersen
 *
 */
public interface IHQLCodeAssist {

	/**
	 * 
	 * @param query the query string (full or partial)
	 * @param position the cursor position inside the query string  
	 * @param requestor requestor on which the codeassist will call methods with information about proposals.
	 */
	void codeComplete(String query, int position, IHQLCompletionRequestor requestor);	

}
