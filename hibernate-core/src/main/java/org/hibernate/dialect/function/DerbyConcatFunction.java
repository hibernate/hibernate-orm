/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.Iterator;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * A specialized concat() function definition in which:<ol>
 *     <li>we translate to use the concat operator ('||')</li>
 *     <li>wrap dynamic parameters in CASTs to VARCHAR</li>
 * </ol>
 * <p/>
 * This last spec is to deal with a limitation on DB2 and variants (e.g. Derby)
 * where dynamic parameters cannot be used in concatenation unless they are being
 * concatenated with at least one non-dynamic operand.  And even then, the rules
 * are so convoluted as to what is allowed and when the CAST is needed and when
 * it is not that we just go ahead and do the CASTing.
 *
 * @author Steve Ebersole
 */
public class DerbyConcatFunction implements SQLFunction {
	/**
	 * {@inheritDoc}
	 * <p/>
	 * Here we always return <tt>true</tt>
	 */
	@Override
	public boolean hasArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Here we always return <tt>true</tt>
	 */
	@Override
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Here we always return {@link StandardBasicTypes#STRING}.
	 */
	@Override
	public Type getReturnType(Type argumentType, Mapping mapping) throws QueryException {
		return StandardBasicTypes.STRING;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Here's the meat..  The whole reason we have a separate impl for this for Derby is to re-define
	 * this method.  The logic here says that if not all the incoming args are dynamic parameters
	 * (i.e. <tt>?</tt>) then we simply use the Derby concat operator (<tt>||</tt>) on the unchanged
	 * arg elements.  However, if all the args are dynamic parameters, then we need to wrap the individual
	 * arg elements in <tt>cast</tt> function calls, use the concatenation operator on the <tt>cast</tt>
	 * returns, and then wrap that whole thing in a call to the Derby <tt>varchar</tt> function.
	 */
	@Override
	public String render(Type argumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		// first figure out if all arguments are dynamic (jdbc parameters) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		boolean areAllArgumentsDynamic = true;
		for ( Object arg1 : args ) {
			final String arg = (String) arg1;
			if ( !"?".equals( arg ) ) {
				// we found a non-dynamic argument
				areAllArgumentsDynamic = false;
				break;
			}
		}

		if ( areAllArgumentsDynamic ) {
			return join(
					args.iterator(),
					CAST_STRING_TRANSFORMER,
					new StringJoinTemplate() {
						public String getBeginning() {
							return "varchar( ";
						}
						public String getSeparator() {
							return " || ";
						}
						public String getEnding() {
							return " )";
						}
					}
			);
		}
		else {
			return join(
					args.iterator(),
					NO_TRANSFORM_STRING_TRANSFORMER,
					new StringJoinTemplate() {
						public String getBeginning() {
							return "(";
						}
						public String getSeparator() {
							return "||";
						}
						public String getEnding() {
							return ")";
						}
					}
			);
		}
	}

	private static interface StringTransformer {
		/**
		 * Transform a string to another
		 *
		 * @param string The String to be transformed
		 *
		 * @return The transformed form
		 */
		public String transform(String string);
	}

	private static final StringTransformer CAST_STRING_TRANSFORMER = new StringTransformer() {
		@Override
		public String transform(String string) {
			// expectation is that incoming string is "?"
			return "cast( ? as varchar(32672) )";
		}
	};

	private static final StringTransformer NO_TRANSFORM_STRING_TRANSFORMER = new StringTransformer() {
		@Override
		public String transform(String string) {
			return string;
		}
	};

	private static interface StringJoinTemplate {
		/**
		 * Getter for property 'beginning'.
		 *
		 * @return Value for property 'beginning'.
		 */
		public String getBeginning();
		/**
		 * Getter for property 'separator'.
		 *
		 * @return Value for property 'separator'.
		 */
		public String getSeparator();
		/**
		 * Getter for property 'ending'.
		 *
		 * @return Value for property 'ending'.
		 */
		public String getEnding();
	}

	private static String join(Iterator/*<String>*/ elements, StringTransformer elementTransformer, StringJoinTemplate template) {
		// todo : make this available via StringHelper?
		final StringBuilder buffer = new StringBuilder( template.getBeginning() );
		while ( elements.hasNext() ) {
			final String element = (String) elements.next();
			buffer.append( elementTransformer.transform( element ) );
			if ( elements.hasNext() ) {
				buffer.append( template.getSeparator() );
			}
		}
		return buffer.append( template.getEnding() ).toString();
	}
}
