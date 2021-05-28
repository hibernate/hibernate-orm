package org.hibernate.query.spi;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloseOnTerminalDecoratorTest extends BaseUnitTestCase {

    @Test
    public void test() {
        testTerminal( DoubleStream::close );

        testTerminal( stream -> stream.forEach( d -> {} ));
        testTerminal( stream -> stream.forEachOrdered( d -> {} ));

        testTerminal( DoubleStream::toArray );
        testTerminal( stream -> stream.reduce( 0.0, Double::sum ) );
        testTerminal( stream -> stream.collect( ArrayList::new, ArrayList::add, ArrayList::addAll ));

        testTerminal( DoubleStream::sum );
        testTerminal( DoubleStream::min );
        testTerminal( DoubleStream::max );
        testTerminal( DoubleStream::count );
        testTerminal( DoubleStream::average );
        testTerminal( DoubleStream::summaryStatistics );

        testTerminal( stream -> stream.anyMatch( d -> d > 0 ) );
        testTerminal( stream -> stream.allMatch( d -> d > 0 ) );
        testTerminal( stream -> stream.noneMatch( d -> d > 0 ) );

        testTerminal( DoubleStream::findFirst );
        testTerminal( DoubleStream::findAny );

        // For exception in terminal operation
        testTerminal( stream -> stream.peek( d -> {
            throw new IllegalStateException();
        }).count(), e -> assertThat( e ).isInstanceOf( IllegalStateException.class ) );
    }

    private void testTerminal(Consumer<DoubleStream> terminal) {
        testTerminal( terminal, e -> {} );
    }

    private void testTerminal(Consumer<DoubleStream> terminal, Consumer<Exception> exceptionHandler) {
        AtomicInteger counter1 = new AtomicInteger();
        AtomicInteger counter2 = new AtomicInteger();
        AtomicInteger counter3 = new AtomicInteger();
        AtomicInteger counter4 = new AtomicInteger();
        AtomicInteger counter5 = new AtomicInteger();
        AtomicInteger counter6 = new AtomicInteger();

        Stream<Integer> stream = Stream.of( 1, 2, 3 )
                .onClose( counter1::incrementAndGet );

        DoubleStream decoratedStream = CloseOnTerminalDecorator.decorate( stream )
                .onClose( counter2::incrementAndGet )
                .map( i -> i * 2 )
                .onClose( counter3::incrementAndGet )
                .mapToInt( Integer::intValue )
                .onClose( counter4::incrementAndGet )
                .asLongStream()
                .onClose( counter5::incrementAndGet )
                .asDoubleStream()
                .onClose( counter6::incrementAndGet );

        try {
            terminal.accept( decoratedStream );
        } catch ( Exception e ) {
            exceptionHandler.accept( e );
        }

        assertThat( counter1 ).hasValue( 1 );
        assertThat( counter2 ).hasValue( 1 );
        assertThat( counter3 ).hasValue( 1 );
        assertThat( counter4 ).hasValue( 1 );
        assertThat( counter5 ).hasValue( 1 );
        assertThat( counter6 ).hasValue( 1 );
    }

}
