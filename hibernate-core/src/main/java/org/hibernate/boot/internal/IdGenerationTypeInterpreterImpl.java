/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.GenerationType;

import org.hibernate.boot.model.IdGenerationTypeInterpreter;
import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.UUIDGenerator;

/**
 * The root (composition) IdGenerationTypeInterpreter.
 *
 * @author Steve Ebersole
 */
public class IdGenerationTypeInterpreterImpl implements IdGenerationTypeInterpreter {
	private IdGenerationTypeInterpreter fallbackInterpreter = FallbackInterpreter.INSTANCE;
	private ArrayList<IdGenerationTypeInterpreter> delegates;

	@Override
	public String determineGeneratorName(GenerationType generationType, Context context) {
		if ( delegates != null ) {
			for ( IdGenerationTypeInterpreter delegate : delegates ) {
				final String result = delegate.determineGeneratorName( generationType, context );
				if ( result != null ) {
					return result;
				}
			}
		}
		return fallbackInterpreter.determineGeneratorName( generationType, context );
	}

	public void enableLegacyFallback() {
		fallbackInterpreter = LegacyFallbackInterpreter.INSTANCE;
	}

	public void disableLegacyFallback() {
		fallbackInterpreter = FallbackInterpreter.INSTANCE;
	}

	public void addInterpreterDelegate(IdGenerationTypeInterpreter delegate) {
		if ( delegates == null ) {
			delegates = new ArrayList<IdGenerationTypeInterpreter>();
		}
		delegates.add( delegate );
	}

	private static class LegacyFallbackInterpreter implements IdGenerationTypeInterpreter {
		/**
		 * Singleton access
		 */
		public static final LegacyFallbackInterpreter INSTANCE = new LegacyFallbackInterpreter();

		@Override
		public String determineGeneratorName(GenerationType generationType, Context context) {
			switch ( generationType ) {
				case IDENTITY: {
					return "identity";
				}
				case SEQUENCE: {
					return "seqhilo";
				}
				case TABLE: {
					return MultipleHiLoPerTableGenerator.class.getName();
				}
				default: {
					// AUTO
					final Class javaType = context.getIdType();
					if ( UUID.class.isAssignableFrom( javaType ) ) {
						return UUIDGenerator.class.getName();
					}
					else {
						return "native";
					}
				}
			}
		}
	}

	private static class FallbackInterpreter implements IdGenerationTypeInterpreter {
		/**
		 * Singleton access
		 */
		public static final FallbackInterpreter INSTANCE = new FallbackInterpreter();

		@Override
		public String determineGeneratorName(GenerationType generationType, Context context) {
			switch ( generationType ) {
				case IDENTITY: {
					return "identity";
				}
				case SEQUENCE: {
					return org.hibernate.id.enhanced.SequenceStyleGenerator.class.getName();
				}
				case TABLE: {
					return org.hibernate.id.enhanced.TableGenerator.class.getName();
				}
				default: {
					// AUTO
					final Class javaType = context.getIdType();
					if ( UUID.class.isAssignableFrom( javaType ) ) {
						return UUIDGenerator.class.getName();
					}
					else {
						return org.hibernate.id.enhanced.SequenceStyleGenerator.class.getName();
					}
				}
			}
		}
	}
}
