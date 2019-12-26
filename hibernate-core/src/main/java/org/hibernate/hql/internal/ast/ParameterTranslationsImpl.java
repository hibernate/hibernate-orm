/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.hql.spi.NamedParameterInformation;
import org.hibernate.hql.spi.ParameterTranslations;
import org.hibernate.hql.spi.PositionalParameterInformation;
import org.hibernate.param.NamedParameterSpecification;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.param.PositionalParameterSpecification;

/**
 * Defines the information available for parameters encountered during
 * query translation through the antlr-based parser.
 *
 * @author Steve Ebersole
 */
public class ParameterTranslationsImpl implements ParameterTranslations {
	private final Map<String,NamedParameterInformationImpl> namedParameters;
	private final Map<Integer,PositionalParameterInformationImpl > ordinalParameters;
	/**
	 * Constructs a parameter metadata object given a list of parameter
	 * specifications.
	 * </p>
	 * Note: the order in the incoming list denotes the parameter's
	 * pseudo-position within the resulting sql statement.
	 *
	 * @param parameterSpecifications The parameter specifications
	 */
	ParameterTranslationsImpl(List<ParameterSpecification> parameterSpecifications) {
		Map<String, NamedParameterInformationImpl> namedParameters = null;
		Map<Integer, PositionalParameterInformationImpl> ordinalParameters = null;

		int i = 0;
		for ( ParameterSpecification specification : parameterSpecifications ) {
			if ( PositionalParameterSpecification.class.isInstance( specification ) ) {
				if ( ordinalParameters == null ) {
					ordinalParameters = new HashMap<>();
				}

				final PositionalParameterSpecification ordinalSpecification = (PositionalParameterSpecification) specification;
				final PositionalParameterInformationImpl info = getPositionalParameterInfo( ordinalParameters, ordinalSpecification );
				info.addSourceLocation( i++ );
			}
			else if ( NamedParameterSpecification.class.isInstance( specification ) ) {
				if ( namedParameters == null ) {
					namedParameters = new HashMap<>();
				}

				final NamedParameterSpecification namedSpecification = (NamedParameterSpecification) specification;
				final NamedParameterInformationImpl info = getNamedParameterInfo( namedParameters, namedSpecification );

				/*
					If a previous reference to the NamedParameter already exists with expected type null and the new
					reference expected type is not null then we add the type to the NamedParameterInformation.

					This situation may occur when the same name parameter is used twice in the same query,
					an example is ".... where :name is null or name.last = :name"

					If info.getExpectedType() != null and also != namedSpecification.getExpectedType(), should an exception be thrown?
				*/
				if ( info.getExpectedType() == null && namedSpecification.getExpectedType() != null ) {
					info.setExpectedType( namedSpecification.getExpectedType() );
				}
				info.addSourceLocation( i++ );
			}
		}

		if ( namedParameters == null ) {
			this.namedParameters = Collections.emptyMap();
		}
		else {
			this.namedParameters = Collections.unmodifiableMap( namedParameters );
		}

		if ( ordinalParameters == null ) {
			this.ordinalParameters = Collections.emptyMap();
		}
		else {
			this.ordinalParameters = Collections.unmodifiableMap( ordinalParameters );
		}
	}

	private NamedParameterInformationImpl getNamedParameterInfo(
			Map<String, NamedParameterInformationImpl> namedParameters,
			NamedParameterSpecification namedSpecification) {
		final String name = namedSpecification.getName();
		NamedParameterInformationImpl namedParameterInformation = namedParameters.get( name );
		if ( namedParameterInformation == null ) {
			namedParameterInformation = new NamedParameterInformationImpl( name, namedSpecification.getExpectedType() );
			namedParameters.put( name,  namedParameterInformation );
		}
		return namedParameterInformation;
	}

	private static PositionalParameterInformationImpl getPositionalParameterInfo(
			Map<Integer, PositionalParameterInformationImpl> ordinalParameters,
			PositionalParameterSpecification ordinalSpecification) {
		final Integer label = Integer.valueOf( ordinalSpecification.getLabel() );
		PositionalParameterInformationImpl positionalParameterInformation = ordinalParameters.get( label );
		if ( positionalParameterInformation == null ) {
			positionalParameterInformation = new PositionalParameterInformationImpl( label, ordinalSpecification.getExpectedType() );
			ordinalParameters.put( label, positionalParameterInformation );
		}
		return positionalParameterInformation;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map getNamedParameterInformationMap() {
		return namedParameters;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map getPositionalParameterInformationMap() {
		return ordinalParameters;
	}

	@Override
	public PositionalParameterInformation getPositionalParameterInformation(int position) {
		return ordinalParameters.get( position );
	}

	@Override
	public NamedParameterInformation getNamedParameterInformation(String name) {
		return namedParameters.get( name );
	}
}
