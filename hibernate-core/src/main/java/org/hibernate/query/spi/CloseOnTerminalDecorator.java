package org.hibernate.query.spi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.*;

public class CloseOnTerminalDecorator implements InvocationHandler {

	private final BaseStream<?, ?> delegate;

	private CloseOnTerminalDecorator(
			BaseStream<?, ?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		boolean terminal;

		switch ( method.getName() ) {
			case "equals":
				// Only consider equal when proxies are identical.
				return proxy == args[0];
			case "hashCode":
				// Use hashCode of proxy.
				return System.identityHashCode( proxy );
			case "toString":
				return "CloseOnTerminal{" + delegate + "}";

			case "forEach":
			case "forEachOrdered":
			case "toArray":
			case "reduce":
			case "collect":
			case "sum":
			case "min":
			case "max":
			case "count":
			case "average":
			case "summaryStatistics":
			case "anyMatch":
			case "allMatch":
			case "noneMatch":
			case "findFirst":
			case "findAny":
				terminal = true;
				break;
			default:
				terminal = false;
				break;
		}

		Object result;
		try {
			result = method.invoke( delegate, args );
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		} finally {
			// Close on terminal
			if ( terminal ) {
				delegate.close();
			}
		}

		if ( result != null && !terminal ) {
			if ( delegate == result ) {
				result = proxy;

			} else if ( result instanceof Stream ) {
				result = decorate( (Stream<?>) result );

			} else if ( result instanceof IntStream ) {
				result = decorate( (IntStream) result );

			} else if ( result instanceof LongStream ) {
				result = decorate( (LongStream) result );

			} else if ( result instanceof DoubleStream ) {
				result = decorate( (DoubleStream) result );
			}
		}

		return result;
	}

	private static final Class<?>[] STREAM = new Class<?>[] { Stream.class };

	@SuppressWarnings("unchecked")
	public static <T> Stream<T> decorate(Stream<T> delegate) {
		return (Stream<T>) Proxy.newProxyInstance(
				CloseOnTerminalDecorator.class.getClassLoader(),
				STREAM,
				new CloseOnTerminalDecorator( delegate )
		);
	}

	private static final Class<?>[] INT_STREAM = new Class<?>[] { IntStream.class };

	public static IntStream decorate(IntStream delegate) {
		return (IntStream) Proxy.newProxyInstance(
				CloseOnTerminalDecorator.class.getClassLoader(),
				INT_STREAM,
				new CloseOnTerminalDecorator( delegate )
		);
	}

	private static final Class<?>[] LONG_STREAM = new Class<?>[] { LongStream.class };

	public static LongStream decorate(LongStream delegate) {
		return (LongStream) Proxy.newProxyInstance(
				CloseOnTerminalDecorator.class.getClassLoader(),
				LONG_STREAM,
				new CloseOnTerminalDecorator( delegate )
		);
	}

	private static final Class<?>[] DOUBLE_STREAM = new Class<?>[] { DoubleStream.class };

	public static DoubleStream decorate(DoubleStream delegate) {
		return (DoubleStream) Proxy.newProxyInstance(
				CloseOnTerminalDecorator.class.getClassLoader(),
				DOUBLE_STREAM,
				new CloseOnTerminalDecorator( delegate )
		);
	}
}
