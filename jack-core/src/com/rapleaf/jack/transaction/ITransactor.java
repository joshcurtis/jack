package com.rapleaf.jack.transaction;

import java.io.Closeable;

import com.rapleaf.jack.IDb;

public interface ITransactor<DB extends IDb> extends Closeable {

  <T> T query(IQuery<DB, T> query);

  <T> T queryAsTransaction(IQuery<DB, T> query);

  void execute(IExecution<DB> execution);

  void executeAsTransaction(IExecution<DB> execution);

  void readAsTransaction(IExecution<DB> execution);

  <T> T readAsTransaction(IQuery<DB, T> query);

  @Override
  void close();

  interface Builder<DB extends IDb, Impl extends ITransactor<DB>> {
    Impl get();
  }
}
