/**
 * 19-MAR-2012
 * 
 * Kiev, Ukraine.
 * 
 */
package com.droidek.core.util;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author Alexander EVseenko
 *
 */
public class Ptr<T> {
  public static final Ptr<?> NULL = new Ptr<Object>(null);
  @SuppressWarnings("unchecked")
  public static final Ptr<String> NULL_STR = (Ptr<String>) NULL;
  @SuppressWarnings("unchecked")
  public static final Ptr<Long> NULL_ID = (Ptr<Long>) NULL;
  @SuppressWarnings("unchecked")
  public static final Ptr<Integer> NULL_INT = (Ptr<Integer>) NULL;
  @SuppressWarnings("unchecked")
  public static final Ptr<Float> NULL_FLT = (Ptr<Float>) NULL;
  @SuppressWarnings("unchecked")
  public static final Ptr<Double> NULL_DBL = (Ptr<Double>) NULL;
  @SuppressWarnings("unchecked")
  public static final Ptr<BigDecimal> NULL_MONEY = (Ptr<BigDecimal>) NULL;
  @SuppressWarnings("unchecked")
  public static final Ptr<Date> NULL_DATE = (Ptr<Date>) NULL;


  public final T value;

  public Ptr(final T value) {
    this.value = value;
  }

  public boolean isNull() {
    return (value == null);
  }

  public boolean isDefined() {
    return (value != null);
  }

  public T getValue(T nullValue) {
    return (isDefined() ? value : nullValue);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Ptr) {
      return (isDefined() ? value.equals(((Ptr<?>) o).value) : false);
    }

    // if right parameter isn't a Ptr then it's a raw type
    return (isDefined() ? value.equals(o) : false);
  }
}
