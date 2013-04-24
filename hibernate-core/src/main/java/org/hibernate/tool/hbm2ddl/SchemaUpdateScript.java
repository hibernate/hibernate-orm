/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.tool.hbm2ddl;

import java.util.ArrayList;
import java.util.List;

/**
 * Pairs a SchemaUpdate SQL script with the boolean 'quiet'.  If true, it allows
 * the script to be run, ignoring all exceptions.
 * 
 * @author Brett Meyer
 */
public class SchemaUpdateScript {
	
	private final String script;
	
	private final boolean quiet;
	
	public SchemaUpdateScript(String script, boolean quiet) {
		this.script = script;
		this.quiet = quiet;
	}

	public String getScript() {
		return script;
	}

	public boolean isQuiet() {
		return quiet;
	}
	
	public static String[] toStringArray(List<SchemaUpdateScript> scripts) {
		String[] scriptsArray = new String[scripts.size()];
		for (int i = 0; i < scripts.size(); i++) {
			scriptsArray[i] = scripts.get( i ).getScript();
		}
		return scriptsArray;
	}
	
	public static List<SchemaUpdateScript> fromStringArray(String[] scriptsArray, boolean quiet) {
		List<SchemaUpdateScript> scripts = new ArrayList<SchemaUpdateScript>();
		for (String script : scriptsArray) {
			scripts.add( new SchemaUpdateScript( script, quiet ) );
		}
		return scripts;
	}
}
