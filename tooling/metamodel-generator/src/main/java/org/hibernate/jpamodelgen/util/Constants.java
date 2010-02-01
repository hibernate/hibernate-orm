// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Hardy Ferentschik
 */
public class Constants {
	public static Map<String, String> COLLECTIONS = new HashMap<String, String>();

	static {
		COLLECTIONS.put( "java.util.Collection", "javax.persistence.metamodel.CollectionAttribute" );
		COLLECTIONS.put( "java.util.Set", "javax.persistence.metamodel.SetAttribute" );
		COLLECTIONS.put( "java.util.List", "javax.persistence.metamodel.ListAttribute" );
		COLLECTIONS.put( "java.util.Map", "javax.persistence.metamodel.MapAttribute" );
	}

	public static List<String> BASIC_TYPES = new ArrayList<String>();

	static {
		BASIC_TYPES.add( "java.lang.String" );
		BASIC_TYPES.add( "java.lang.Boolean" );
		BASIC_TYPES.add( "java.lang.Byte" );
		BASIC_TYPES.add( "java.lang.Character" );
		BASIC_TYPES.add( "java.lang.Short" );
		BASIC_TYPES.add( "java.lang.Integer" );
		BASIC_TYPES.add( "java.lang.Long" );
		BASIC_TYPES.add( "java.lang.Float" );
		BASIC_TYPES.add( "java.lang.Double" );
		BASIC_TYPES.add( "java.math.BigInteger" );
		BASIC_TYPES.add( "java.math.BigDecimal" );
		BASIC_TYPES.add( "java.util.Date" );
		BASIC_TYPES.add( "java.util.Calendar" );
		BASIC_TYPES.add( "java.sql.Date" );
		BASIC_TYPES.add( "java.sql.Time" );
		BASIC_TYPES.add( "java.sql.Timestamp" );
	}

	public static List<String> BASIC_ARRAY_TYPES = new ArrayList<String>();

	static {
		BASIC_ARRAY_TYPES.add( "java.lang.Character" );
		BASIC_ARRAY_TYPES.add( "java.lang.Byte" );
	}

	private Constants(){}
}


