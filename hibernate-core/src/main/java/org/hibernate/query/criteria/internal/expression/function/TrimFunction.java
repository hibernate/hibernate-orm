/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression.function;

import java.io.Serializable;
import javax.persistence.criteria.CriteriaBuilder.Trimspec;
import javax.persistence.criteria.Expression;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.ParameterRegistry;
import org.hibernate.query.criteria.internal.Renderable;
import org.hibernate.query.criteria.internal.compile.RenderingContext;
import org.hibernate.query.criteria.internal.expression.LiteralExpression;

/**
 * Models the ANSI SQL <tt>TRIM</tt> function.
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class TrimFunction
		extends BasicFunctionExpression<String>
		implements Serializable {
	public static final String NAME = "trim";
	public static final Trimspec DEFAULT_TRIMSPEC = Trimspec.BOTH;
	public static final char DEFAULT_TRIM_CHAR = ' ';

	private final Trimspec trimspec;
	private final Expression<Character> trimCharacter;
	private final Expression<String> trimSource;

	public TrimFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Trimspec trimspec,
			Expression<Character> trimCharacter,
			Expression<String> trimSource) {
		super( criteriaBuilder, String.class, NAME );
		this.trimspec = trimspec;
		this.trimCharacter = trimCharacter;
		this.trimSource = trimSource;
	}

	public TrimFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Trimspec trimspec,
			char trimCharacter,
			Expression<String> trimSource) {
		super( criteriaBuilder, String.class, NAME );
		this.trimspec = trimspec;
		this.trimCharacter = new LiteralExpression<Character>( criteriaBuilder, trimCharacter );
		this.trimSource = trimSource;
	}

	public TrimFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<String> trimSource) {
		this( criteriaBuilder, DEFAULT_TRIMSPEC, DEFAULT_TRIM_CHAR, trimSource );
	}

	public TrimFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Expression<Character> trimCharacter,
			Expression<String> trimSource) {
		this( criteriaBuilder, DEFAULT_TRIMSPEC, trimCharacter, trimSource );
	}

	public TrimFunction(
			CriteriaBuilderImpl criteriaBuilder,
			char trimCharacter,
			Expression<String> trimSource) {
		this( criteriaBuilder, DEFAULT_TRIMSPEC, trimCharacter, trimSource );
	}

	public TrimFunction(
			CriteriaBuilderImpl criteriaBuilder,
			Trimspec trimspec,
			Expression<String> trimSource) {
		this( criteriaBuilder, trimspec, DEFAULT_TRIM_CHAR, trimSource );
	}

	public Expression<Character> getTrimCharacter() {
		return trimCharacter;
	}

	public Expression<String> getTrimSource() {
		return trimSource;
	}

	public Trimspec getTrimspec() {
		return trimspec;
	}

	@Override
	public void registerParameters(ParameterRegistry registry) {
		Helper.possibleParameter( getTrimCharacter(), registry );
		Helper.possibleParameter( getTrimSource(), registry );
	}

	@Override
	public String render(RenderingContext renderingContext) {
		renderingContext.getFunctionStack().push( this );

		try {
			String renderedTrimChar;
			if ( trimCharacter.getClass().isAssignableFrom( LiteralExpression.class ) ) {
				// If the character is a literal, treat it as one.  A few dialects
				// do not support parameters as trim() arguments.
				renderedTrimChar = '\'' + ( (LiteralExpression<Character>)
						trimCharacter ).getLiteral().toString() + '\'';
			}
			else {
				renderedTrimChar = ( (Renderable) trimCharacter ).render( renderingContext );
			}
			return new StringBuilder()
					.append( "trim(" )
					.append( trimspec.name() )
					.append( ' ' )
					.append( renderedTrimChar )
					.append( " from " )
					.append( ( (Renderable) trimSource ).render( renderingContext ) )
					.append( ')' )
					.toString();
		}
		finally {
			renderingContext.getFunctionStack().pop();
		}
	}
}
