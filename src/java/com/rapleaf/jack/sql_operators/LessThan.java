package com.rapleaf.jack.sql_operators;

import com.rapleaf.jack.SqlOperator;

public class LessThan<N extends Comparable<N>> extends SqlOperator<N> {

  public LessThan(N number) {
    super(number);
    if (number == null) {
      throw new IllegalArgumentException("You cannot pass null parameters.");
    }
  }

  @Override
  public String getSqlStatement() {
    return " < ? ";
  }

  @Override
  public boolean apply(N value) {
    return value.compareTo(getParameter()) < 0;
  }

  @Override
  public int getNbNotNullParameters() {
    return 1;
  }

  public N getParameter() {
    return getParameters().get(0);
  }
}
