/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.testing.sql;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 */
// TODO Provide mechanism to allow for expected failures (like DML against tables that don't exist)
public class SqlComparator {

	static final Comparator< Object > COMPARATOR = new Comparator< Object >() {

		@Override
		public int compare( Object object1, Object object2 ) {
			return object1.toString().compareToIgnoreCase( object2.toString() );
		}
	};

	Map< String, String > namesByExpectedName = new HashMap< String, String >();

	public void clear() {
		namesByExpectedName.clear();
	}

	public void compare( List< Statement > statements, List< Statement > expectedStatements ) {
		for ( Iterator< Statement > iter = statements.iterator(); iter.hasNext(); ) {
			Statement statement = iter.next();
			boolean matched = false;
			if ( statement instanceof NamedObject ) {
				for ( Iterator< Statement > expectedIter = expectedStatements.iterator(); expectedIter.hasNext(); ) {
					Statement expectedStatement = expectedIter.next();
					if ( statement.getClass() == expectedStatement.getClass()
							&& ( ( NamedObject ) statement ).name().unquoted().equalsIgnoreCase(
									( ( NamedObject ) expectedStatement ).name().unquoted() ) ) {
						matched = compare( statement, expectedStatement, iter, expectedIter );
						if ( matched ) {
							break;
						}
					}
				}
			}
			if ( !matched ) {
				for ( Iterator< Statement > expectedIter = expectedStatements.iterator(); expectedIter.hasNext(); ) {
					if ( compare( statement, expectedIter.next(), iter, expectedIter ) ) {
						break;
					}
				}
			}
		}
	}

	private boolean compare(
			Statement statement,
			Statement expectedStatement,
			Iterator< Statement > iterator,
			Iterator< Statement > expectedIterator ) {
		CompareVisitor compareVisitor = new CompareVisitor( expectedStatement );
		SqlWalker.INSTANCE.walk( compareVisitor, statement );
		if ( compareVisitor.comparison == 0 ) {
			SqlWalker.INSTANCE.walk( new MapVisitor( expectedStatement ), statement );
			iterator.remove();
			expectedIterator.remove();
			return true;
		}
		return false;
	}

	private class CompareVisitor extends Visitor {

		int comparison;

		CompareVisitor( Object expectedObject ) {
			super( expectedObject );
		}

		boolean compareReferents( Reference reference, Reference expectedReference ) {
			if ( reference.referent instanceof Alias ) {
				if ( ( ( Alias ) reference.referent ).reference == null ) {
					if ( ( ( Alias ) expectedReference.referent ).reference == null ) {
						return true;
					}
					comparison = -1;
					return false;
				}
				comparison = 1;
				return false;
			}
			String text = ( ( NamedObject ) reference.referent ).name().unquoted();
			String expectedText = ( ( NamedObject ) expectedReference.referent ).name().unquoted();
			if ( reference.referent.getClass() == Column.class ) {
				text = ( ( CreateTable ) reference.referent.parent() ).name().unquoted() + '.' + text;
				expectedText = ( ( CreateTable ) expectedReference.referent.parent() ).name().unquoted() + '.' + expectedText;
			}
			String matchingText = namesByExpectedName.get( expectedText );
			if ( matchingText == null ) {
				// Check if both are self-references
				if ( ( reference.referent == reference.statement() && expectedReference.referent == expectedReference.statement() )
						|| ( reference.referent instanceof Column && expectedReference.referent instanceof Column ) ) {
					return true;
				}
				comparison = 1;
				return false;
			}
			comparison = text.compareToIgnoreCase( matchingText );
			return comparison == 0;
		}

		private boolean compareTypes( Object object, Object expectedObject ) {
			Class< ? > objClass = object.getClass();
			Class< ? > expectedObjClass = expectedObject.getClass();
			if ( objClass != expectedObjClass ) {
				comparison = objClass.getSimpleName().compareTo( expectedObjClass.getSimpleName() );
				return false;
			}
			return true;
		}

		private Boolean compareWithRespectToNull( Object object, Object expectedObject ) {
			if ( object == null ) {
				if ( expectedObject == null ) {
					return true;
				}
				comparison = -1;
				return false;
			}
			if ( expectedObject == null ) {
				comparison = 1;
				return false;
			}
			return null;
		}

		protected boolean visit( String text, String expectedText ) {
			return true;
		}

		@Override
		public boolean visit( Object object, SqlObject parent, Field field, int index ) {
			Object expectedObj = expectedObject( field, index );
			Boolean result = compareWithRespectToNull( object, expectedObj );
			if ( result != null ) {
				return result;
			}
			if ( !compareTypes( object, expectedObj ) ) {
				return false;
			}
			if ( object instanceof List ) {
				List< Object > list = ( List< Object > ) object;
				List< Object > expectedList = ( List< Object > ) expectedObj;
				comparison = list.size() - expectedList.size();
				if ( comparison != 0 ) {
					return false;
				}
				if ( list instanceof OptionallyOrderedSet ) {
					Collections.sort( list, COMPARATOR );
					Collections.sort( expectedList, COMPARATOR );
				}
				return true;
			}
			if ( object instanceof String ) {
				if ( field.getName().equals( "text" ) ) {
					if ( parent instanceof Name ) {
						return visit( object.toString(), expectedObj.toString() );
					}
					if ( parent instanceof Literal ) {
						comparison = Literal.unquoted( object.toString() ).compareTo( Literal.unquoted( expectedObj.toString() ) );
						return comparison == 0;
					}
				}
				comparison = object.toString().compareToIgnoreCase( expectedObj.toString() );
				return comparison == 0;
			}
			if ( object instanceof Boolean ) {
				comparison = ( ( Boolean ) object ).compareTo( ( Boolean ) expectedObj );
				return comparison == 0;
			}
			if ( object instanceof Integer ) {
				comparison = ( ( Integer ) object ).compareTo( ( Integer ) expectedObj );
				return comparison == 0;
			}
			if ( object instanceof Reference ) {
				Reference ref = ( Reference ) object;
				Reference expectedRef = ( Reference ) expectedObj;
				if ( ref.text.startsWith( "HT_" ) ) { // Temporary table
					if ( expectedRef.referent == null ) {
						comparison = 1;
					} else if ( expectedRef.text.startsWith( "HT_" ) ) {
						comparison = ref.text.compareToIgnoreCase( expectedRef.text );
					} else {
						comparison = -1;
					}
					return comparison == 0;
				}
				if ( !compareTypes( ref.referent, expectedRef.referent ) ) {
					return false;
				}
				if ( ref.referent instanceof NamedObject ) {
					compareReferents( ref, expectedRef );
				} else if ( ref.referent instanceof Alias ) {
					ref = ( ( Alias ) ref.referent ).reference;
					expectedRef = ( ( Alias ) expectedRef.referent ).reference;
					result = compareWithRespectToNull( ref, expectedRef );
					if ( result != null ) {
						return result;
					}
					if ( ref.referent != null || expectedRef.referent != null ) {
						compareReferents( ref, expectedRef );
					}
				} else {
					throw new RuntimeException( "Not yet implemented" );
				}
				return comparison == 0;
			}
			return true;
		}
	}

	private class MapVisitor extends Visitor {

		MapVisitor( Object expectedObject ) {
			super( expectedObject );
		}

		@Override
		public boolean visit( Object object, SqlObject parent, Field field, int index ) {
			Object expectedObj = expectedObject( field, index );
			if ( !( object instanceof Name ) ) {
				return true;
			}
			Name name = ( Name ) object;
			Name expectedName = ( Name ) expectedObj;
			String text = name.unquoted();
			String expectedText = expectedName.unquoted();
			if ( name.parent() instanceof Column ) {
				text = ( ( CreateTable ) name.parent().parent() ).name().unquoted() + '.' + text;
				expectedText = ( ( CreateTable ) expectedName.parent().parent() ).name().unquoted() + '.' + expectedText;
			}
			// If entries don't match exactly, check if we can swap an existing entry as a better match
			boolean trySwap = !text.equalsIgnoreCase( expectedText );
			if ( trySwap && object instanceof Alias ) {
				Alias alias = ( Alias ) object;
				trySwap =
						alias.reference != null
								&& !alias.reference.unquoted().equalsIgnoreCase( ( ( Alias ) expectedObj ).reference.unquoted() );
			}
			if ( trySwap ) {
				String oldText = namesByExpectedName.get( text );
				if ( oldText != null ) {
					namesByExpectedName.put( text, text );
					namesByExpectedName.put( expectedText, oldText );
					return true;
				}
			}
			namesByExpectedName.put( expectedText, text );
			return true;
		}
	}

	private abstract class Visitor extends SqlVisitor {

		private Deque< Object > expectedObjects = new LinkedList< Object >();

		Visitor( Object expectedObject ) {
			expectedObjects.push( expectedObject );
		}

		Object expectedObject( Field field, int index ) {
			if ( index < 0 ) {
				if ( field == null ) {
					return expectedObjects.peek();
				}
				try {
					return field.get( expectedObjects.peek() );
				} catch ( IllegalAccessException error ) {
					throw new RuntimeException( error );
				}
			}
			return ( ( List< Object > ) expectedObjects.peek() ).get( index );
		}

		private boolean postVisit() {
			expectedObjects.pop();
			return true;
		}

		@Override
		protected boolean postVisitElements( List< ? > list, SqlObject parent, Field field, int index ) {
			return postVisit();
		}

		@Override
		protected boolean postVisitFields( SqlObject object, SqlObject parent, Field field, int index ) {
			return postVisit();
		}

		private boolean preVisit( Field field, int index ) {
			if ( index < 0 ) {
				if ( field != null ) {
					try {
						expectedObjects.push( field.get( expectedObjects.peek() ) );
					} catch ( IllegalAccessException error ) {
						throw new RuntimeException( error );
					}
				}
			} else {
				expectedObjects.push( ( ( List< ? > ) expectedObjects.peek() ).get( index ) );
			}
			return true;
		}

		@Override
		protected boolean preVisitElements( List< ? > list, SqlObject parent, Field field, int index ) {
			return preVisit( field, index );
		}

		@Override
		protected boolean preVisitFields( SqlObject object, SqlObject parent, Field field, int index ) {
			return preVisit( field, index );
		}
	}
}
