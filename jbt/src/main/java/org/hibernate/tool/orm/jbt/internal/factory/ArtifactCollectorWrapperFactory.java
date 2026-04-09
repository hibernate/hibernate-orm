/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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
package org.hibernate.tool.orm.jbt.internal.factory;

import java.io.File;
import java.util.Set;

import org.hibernate.tool.reveng.api.export.ArtifactCollector;
import org.hibernate.tool.reveng.internal.export.common.DefaultArtifactCollector;
import org.hibernate.tool.orm.jbt.api.wrp.ArtifactCollectorWrapper;
import org.hibernate.tool.orm.jbt.internal.wrp.AbstractWrapper;

public class ArtifactCollectorWrapperFactory {
	
	public static ArtifactCollectorWrapper createArtifactCollectorWrapper() {
		return new ArtifactCollectorWrapperImpl();
	}
	
	private static class ArtifactCollectorWrapperImpl 
			extends AbstractWrapper
			implements ArtifactCollectorWrapper {
		
		private ArtifactCollector wrappedArtifactCollector = new DefaultArtifactCollector();
		
		@Override 
		public ArtifactCollector getWrappedObject() { 
			return wrappedArtifactCollector; 
		}

		@Override
		public Set<String> getFileTypes() { 
			return ((ArtifactCollector)getWrappedObject()).getFileTypes(); 
		}

		@Override
		public void formatFiles() { 
			((ArtifactCollector)getWrappedObject()).formatFiles(); 
		}
		
		@Override
		public File[] getFiles(String string) { 
			return ((ArtifactCollector)getWrappedObject()).getFiles(string); 
		}
	}
	
}
