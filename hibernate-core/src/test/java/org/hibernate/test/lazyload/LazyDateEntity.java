package org.hibernate.test.lazyload;

import java.util.Date;

import javax.persistence.*;
import javax.persistence.Entity;

import org.hibernate.annotations.*;
import org.hibernate.bytecode.instrumentation.spi.AbstractFieldInterceptor;
import org.hibernate.bytecode.internal.javassist.FieldHandler;

@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LazyDateEntity implements org.hibernate.bytecode.internal.javassist.FieldHandled {
    private Long id;
    private Date date;
    private AbstractFieldInterceptor interceptor;

    @Override
    public void setFieldHandler(final FieldHandler handler) {
        this.interceptor = (AbstractFieldInterceptor) handler;
    }

    @Override
    public FieldHandler getFieldHandler() {
        return (FieldHandler) interceptor;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Basic(fetch = FetchType.LAZY)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getDate() {
        if (interceptor != null && !interceptor.isInitialized("date")) {
            return (Date) getFieldHandler().readObject(this, "date", date);
        } else {
            return date;
        }
    }

    public void setDate(final Date date) {
        if (interceptor == null || interceptor.isInitializing()) {
            this.date = date;
        } else {
            this.date = (Date) getFieldHandler().writeObject(this, "date", this.date, date);
        }
    }
}
