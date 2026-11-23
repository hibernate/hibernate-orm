package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Immutable description of the arithmetic syntax of a Dialect, for the cases
/// where databases differ in how a binary arithmetic expression is written.
///
/// Providers supply a stable profile through
/// [org.hibernate.dialect.Dialect#getArithmeticSupport]. The two attributes
/// are independent.
///
/// @see org.hibernate.dialect.Dialect#getArithmeticSupport()
/// @since 8.0
/// @author Steve Ebersole
@SPI({ USE, SUPPLY })
public final class ArithmeticSupport {
	/// The base-Dialect profile: modulo is a function, and integer division is
	/// what the database's division operator does for integer operands.
	public static final ArithmeticSupport STANDARD =
			new ArithmeticSupport( ModuloForm.FUNCTION, IntegerDivisionForm.NATIVE );

	private final ModuloForm moduloForm;
	private final IntegerDivisionForm integerDivisionForm;

	private ArithmeticSupport(ModuloForm moduloForm, IntegerDivisionForm integerDivisionForm) {
		this.moduloForm = moduloForm;
		this.integerDivisionForm = integerDivisionForm;
	}

	/// Create a builder initialized from [#STANDARD].
	public static Builder builder() {
		return new Builder( STANDARD );
	}

	/// Create a builder initialized from the given profile.
	///
	/// @param base the non-null profile to copy
	public static Builder builder(ArithmeticSupport base) {
		return new Builder( requireArgument( base, "base" ) );
	}

	/// How the remainder of a division is written.
	public ModuloForm getModuloForm() {
		return moduloForm;
	}

	/// How a division of two integers which must yield an integer is written.
	public IntegerDivisionForm getIntegerDivisionForm() {
		return integerDivisionForm;
	}

	/// How the remainder of a division is written.
	public enum ModuloForm {
		/// `mod(a,b)`
		FUNCTION,

		/// `(a%b)`
		OPERATOR
	}

	/// How a division of two integers which must yield an integer is written.
	///
	/// Hibernate asks for this when both operands of a portable division are
	/// integers. A database whose division operator already truncates for
	/// integer operands is [#NATIVE]; the other forms emulate that.
	public enum IntegerDivisionForm {
		/// `(a/b)`
		NATIVE,

		/// `floor(a/b)`
		FLOOR,

		/// `(a div b)`
		DIV_OPERATOR,

		/// `cast(a/b as int)`
		CAST_INTEGER,

		/// `div(a,b)`
		DIV_FUNCTION,

		/// `a \ b`
		BACKSLASH
	}

	/// Build an immutable arithmetic-support profile.
	public static final class Builder {
		private ModuloForm moduloForm;
		private IntegerDivisionForm integerDivisionForm;

		private Builder(ArithmeticSupport base) {
			moduloForm = base.moduloForm;
			integerDivisionForm = base.integerDivisionForm;
		}

		/// Set how the remainder of a division is written.
		public Builder moduloForm(ModuloForm moduloForm) {
			this.moduloForm = requireArgument( moduloForm, "moduloForm" );
			return this;
		}

		/// Set how a division of two integers which must yield an integer is written.
		public Builder integerDivisionForm(IntegerDivisionForm integerDivisionForm) {
			this.integerDivisionForm = requireArgument( integerDivisionForm, "integerDivisionForm" );
			return this;
		}

		/// Build an immutable snapshot of this builder.
		public ArithmeticSupport build() {
			return new ArithmeticSupport( moduloForm, integerDivisionForm );
		}
	}

	private static <T> T requireArgument(T argument, String name) {
		if ( argument == null ) {
			throw new IllegalArgumentException( name + " must not be null" );
		}
		return argument;
	}
}
