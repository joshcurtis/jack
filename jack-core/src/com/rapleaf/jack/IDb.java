//
// Copyright 2011 Rapleaf
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.rapleaf.jack;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.rapleaf.jack.queries.Column;
import com.rapleaf.jack.queries.GenericDeletion;
import com.rapleaf.jack.queries.GenericInsertion;
import com.rapleaf.jack.queries.GenericQuery;
import com.rapleaf.jack.queries.GenericUpdate;
import com.rapleaf.jack.queries.Records;

public interface IDb extends Serializable, Closeable {

  /**
   * Delete all records in every persistence within this database.
   *
   * @return true if and only if each persistence successfully
   *         deletes all its records.
   * @throws IOException
   */
  public boolean deleteAll() throws IOException;

  public void disableCaching();

  public void setAutoCommit(boolean autoCommit);

  public boolean getAutoCommit();

  public void setReadOnly(boolean readOnly);

  public boolean isReadOnly();

  public void commit();

  public void rollback();

  public void resetConnection();

  @Override
  public void close() throws IOException;

  public void setBulkOperation(boolean isAllowBulkOperation);

  public boolean getBulkOperation();

  GenericInsertion.Builder createInsertion();

  GenericQuery.Builder createQuery();

  GenericUpdate.Builder createUpdate();

  GenericDeletion.Builder createDeletion();

  Records findBySql(String statement, List<?> params, Collection<Column> columns) throws IOException;

}
